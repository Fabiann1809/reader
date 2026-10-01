package io.github.fabiann1809.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.book.arrangedBy
import io.github.fabiann1809.reader.data.book.importing.ImportQueue
import io.github.fabiann1809.reader.data.book.importing.ImportStatus
import io.github.fabiann1809.reader.data.book.markedAsRead
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val filter: LibraryFilter = LibraryFilter.Default,
    /** The selected user collection, when [filter] is a custom one. */
    val currentCollection: Collection? = null,
    /** User collections, for the collection picker. */
    val collections: List<Collection> = emptyList(),
    /** True when the user has no books at all (not just none in this collection). */
    val libraryIsEmpty: Boolean = false,
    /** Search text; [books] only holds the matches when it is not blank. */
    val query: String = "",
    /** Shelf order and filters already applied to [books]. */
    val arrangement: LibraryArrangement = LibraryArrangement(),
    val layout: LibraryLayout = LibraryLayout(),
    /** Books checked in selection mode; empty when not selecting. Only ids of shown books. */
    val selectedIds: Set<Long> = emptySet(),
) {
    val isSelecting: Boolean
        get() = selectedIds.isNotEmpty()
}

/** The book whose "Colección" sheet is open, with the user collections that already hold it. */
data class BookCollections(val book: Book, val collectionIds: Set<Long>)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val bookRepository: BookRepository,
    private val collectionRepository: CollectionRepository,
    private val preferences: AppPreferences,
    private val importQueue: ImportQueue,
) : ViewModel() {

    // A custom collection that no longer exists (deleted elsewhere) falls back to "Todos".
    private val filter = preferences.libraryFilter.flatMapLatest { filter ->
        if (filter is LibraryFilter.Custom) {
            collectionRepository.observeCollection(filter.collectionId)
                .map { collection -> if (collection == null) LibraryFilter.Default to null else filter to collection }
        } else {
            flowOf(filter to null)
        }
    }

    // Not persisted: a search only lasts while the library is open.
    private val query = MutableStateFlow("")

    // Not persisted either; selection ends when the user leaves the library.
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<LibraryUiState> = combine(
        filter.flatMapLatest { (filter, collection) ->
            collectionRepository.observeBooks(filter).map { books -> Triple(filter, collection, books) }
        },
        collectionRepository.observeCollections(),
        bookRepository.observeBooks().map { it.isEmpty() },
        query,
        // combine() takes at most five typed flows, so the display state travels together.
        combine(preferences.libraryArrangement, preferences.libraryLayout, selection, ::Triple),
    ) { (filter, collection, books), collections, libraryIsEmpty, query, (arrangement, layout, selection) ->
        val shown = books.filter { it.matchesSearch(query) }.arrangedBy(arrangement)
        LibraryUiState(
            books = shown,
            isLoading = false,
            filter = filter,
            currentCollection = collection,
            collections = collections,
            libraryIsEmpty = libraryIsEmpty,
            query = query,
            arrangement = arrangement,
            layout = layout,
            // Only shown books count: hidden (searched out, filtered, deleted) ones are never acted on.
            selectedIds = shown.mapTo(mutableSetOf()) { it.id }.intersect(selection),
        )
    }.stateIn(
        scope = viewModelScope,
        // Keeps the query alive briefly across configuration changes (e.g. rotation).
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LibraryUiState(),
    )

    /** Filters the shown collection by title and author as the user types. */
    fun search(text: String) {
        query.value = text
    }

    fun setArrangement(arrangement: LibraryArrangement) {
        viewModelScope.launch { preferences.setLibraryArrangement(arrangement) }
    }

    val importStatus: StateFlow<ImportStatus> = importQueue.status

    /** [uris] are the documents picked in the system file picker. New books appear on the shelves by themselves. */
    fun importBooks(uris: List<String>) = importQueue.add(uris)

    fun retryFailedImports() = importQueue.retry()

    fun dismissFailedImports() = importQueue.dismiss()

    private val collectionsBookId = MutableStateFlow<Long?>(null)

    /** Non-null while the "Colección" sheet of a book (opened from its cover menu) is shown. */
    val bookCollections: StateFlow<BookCollections?> = collectionsBookId.flatMapLatest { bookId ->
        if (bookId == null) {
            flowOf(null)
        } else {
            combine(bookRepository.observeBook(bookId), collectionRepository.observeCollectionIdsOf(bookId)) { book, ids ->
                book?.let { BookCollections(it, ids.toSet()) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    fun showCollectionsOf(bookId: Long) {
        collectionsBookId.value = bookId
    }

    fun hideCollections() {
        collectionsBookId.value = null
    }

    /** "Mis favoritos" is backed by the book's favorite flag, not by a stored collection. */
    fun setFavorite(bookId: Long, isFavorite: Boolean) = updateBook(bookId) { it.copy(isFavorite = isFavorite) }

    fun setInCollection(bookId: Long, collectionId: Long, isIncluded: Boolean) {
        viewModelScope.launch {
            if (isIncluded) {
                collectionRepository.addBook(bookId, collectionId)
            } else {
                collectionRepository.removeBook(bookId, collectionId)
            }
        }
    }

    /** Creates a collection that already holds the book, without leaving the current shelf. */
    fun createCollectionWithBook(bookId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val collectionId = collectionRepository.createCollection(name)
            collectionRepository.addBook(bookId, collectionId)
        }
    }

    fun markAsRead(bookId: Long) = updateBook(bookId) { it.markedAsRead() }

    /** Its notes and collection links go with it (database cascade). */
    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.deleteBook(book)
        }
    }

    // Reads the stored book rather than the UI state so a stale shelf can't overwrite newer data.
    private fun updateBook(bookId: Long, change: (Book) -> Book) {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.updateBook(change(book))
        }
    }

    /** Checks or unchecks a book; unchecking the last one ends selection mode. */
    fun toggleSelection(bookId: Long) {
        selection.update { if (bookId in it) it - bookId else it + bookId }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun addSelectedToFavorites() = forEachSelected { bookId ->
        bookRepository.getBook(bookId)?.let { bookRepository.updateBook(it.copy(isFavorite = true)) }
    }

    fun addSelectedToCollection(collectionId: Long) = forEachSelected { bookId ->
        collectionRepository.addBook(bookId, collectionId)
    }

    fun createCollectionWithSelected(name: String) {
        if (name.isBlank()) return
        val bookIds = uiState.value.selectedIds
        clearSelection()
        viewModelScope.launch {
            val collectionId = collectionRepository.createCollection(name)
            bookIds.forEach { collectionRepository.addBook(it, collectionId) }
        }
    }

    fun deleteSelected() = forEachSelected { bookId ->
        bookRepository.getBook(bookId)?.let { bookRepository.deleteBook(it) }
    }

    /** Applies [action] to every selected book, then leaves selection mode. */
    private fun forEachSelected(action: suspend (Long) -> Unit) {
        val bookIds = uiState.value.selectedIds
        clearSelection()
        viewModelScope.launch { bookIds.forEach { action(it) } }
    }

    fun setLayout(layout: LibraryLayout) {
        viewModelScope.launch { preferences.setLibraryLayout(layout) }
    }

    fun selectFilter(filter: LibraryFilter) {
        viewModelScope.launch { preferences.setLibraryFilter(filter) }
    }

    /** Creates a collection and shows it right away. */
    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = collectionRepository.createCollection(name)
            preferences.setLibraryFilter(LibraryFilter.Custom(id))
        }
    }

    fun renameCurrentCollection(name: String) {
        if (name.isBlank()) return
        val collection = uiState.value.currentCollection ?: return
        viewModelScope.launch { collectionRepository.renameCollection(collection, name) }
    }

    /** Deletes the shown collection (its books stay in the library) and goes back to "Todos". */
    fun deleteCurrentCollection() {
        viewModelScope.launch {
            val filter = preferences.libraryFilter.first() as? LibraryFilter.Custom ?: return@launch
            val collection = collectionRepository.observeCollection(filter.collectionId).first() ?: return@launch
            preferences.setLibraryFilter(LibraryFilter.Default)
            collectionRepository.deleteCollection(collection)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
