package io.github.fabiann1809.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.book.arrangedBy
import io.github.fabiann1809.reader.data.book.bookToContinue
import io.github.fabiann1809.reader.data.book.importing.ImportQueue
import io.github.fabiann1809.reader.data.book.importing.ImportStatus
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionGroup
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.collection.buildCollectionGroups
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
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

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val bookRepository: BookRepository,
    private val collectionRepository: CollectionRepository,
    private val preferences: AppPreferences,
    private val importQueue: ImportQueue,
    private val organizer: BookOrganizer,
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
        // The whole library (not just this shelf): is it empty, and which book "Continuar" opens.
        combine(bookRepository.observeBooks(), collectionRepository.observeCollections(), collectionRepository.observeLinks()) { all, collections, links ->
            WholeLibrary(
                isEmpty = all.isEmpty(),
                bookToContinue = all.bookToContinue(),
                smartCounts = all.countBySmartCollection(),
                groups = buildCollectionGroups(all, collections, links),
                books = all,
            )
        },
        query,
        // combine() takes at most five typed flows, so the display state travels together.
        combine(preferences.libraryArrangement, preferences.libraryLayout, selection, ::Triple),
    ) { (filter, collection, books), collections, whole, query, (arrangement, layout, selection) ->
        val shown = books.filter { it.matchesSearch(query) }.arrangedBy(arrangement)
        LibraryUiState(
            books = shown,
            isLoading = false,
            filter = filter,
            currentCollection = collection,
            collections = collections,
            libraryIsEmpty = whole.isEmpty,
            bookToContinue = whole.bookToContinue,
            smartCounts = whole.smartCounts,
            collectionGroups = whole.groups,
            allBooks = whole.books,
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

    private class WholeLibrary(
        val isEmpty: Boolean,
        val bookToContinue: Book?,
        val smartCounts: Map<SmartCollection, Int>,
        val groups: List<CollectionGroup>,
        val books: List<Book>,
    )

    private fun List<Book>.countBySmartCollection() = SmartCollection.entries.associateWith { smart -> count(smart.includes) }

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

    // One book, from its cover menu.

    fun setFavorite(bookId: Long, isFavorite: Boolean) = organize { setFavorite(listOf(bookId), isFavorite) }

    fun setInCollection(bookId: Long, collectionId: Long, isIncluded: Boolean) =
        organize { setInCollection(bookId, collectionId, isIncluded) }

    /** Creates a collection that already holds the book, without leaving the current shelf. */
    fun createCollectionWithBook(bookId: Long, name: String) = organize { createCollectionWith(listOf(bookId), name) }

    fun markAsRead(bookId: Long) = organize { markAsRead(bookId) }

    fun deleteBook(bookId: Long) = organize { delete(listOf(bookId)) }

    private fun organize(action: suspend BookOrganizer.() -> Unit) {
        viewModelScope.launch { organizer.action() }
    }

    /** Checks or unchecks a book; unchecking the last one ends selection mode. */
    fun toggleSelection(bookId: Long) {
        selection.update { if (bookId in it) it - bookId else it + bookId }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun addSelectedToFavorites() = organizeSelected { setFavorite(it, isFavorite = true) }

    fun addSelectedToCollection(collectionId: Long) = organizeSelected { addToCollection(it, collectionId) }

    fun createCollectionWithSelected(name: String) {
        if (name.isBlank()) return
        organizeSelected { createCollectionWith(it, name) }
    }

    fun deleteSelected() = organizeSelected { delete(it) }

    /** Applies [action] to the selected books, then leaves selection mode. */
    private fun organizeSelected(action: suspend BookOrganizer.(Set<Long>) -> Unit) {
        val bookIds = uiState.value.selectedIds
        clearSelection()
        viewModelScope.launch { organizer.action(bookIds) }
    }

    fun setLayout(layout: LibraryLayout) {
        viewModelScope.launch { preferences.setLibraryLayout(layout) }
    }

    fun selectFilter(filter: LibraryFilter) {
        viewModelScope.launch { preferences.setLibraryFilter(filter) }
    }

    /** Renames a collection of the user, not necessarily the shown one. */
    fun renameCollection(collection: Collection, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { collectionRepository.renameCollection(collection, name) }
    }

    /** Deletes a collection of the user (its books stay in the library); if it was shown, goes back to "Todos". */
    fun deleteCollection(collection: Collection) {
        viewModelScope.launch {
            if ((preferences.libraryFilter.first() as? LibraryFilter.Custom)?.collectionId == collection.id) {
                preferences.setLibraryFilter(LibraryFilter.Default)
            }
            collectionRepository.deleteCollection(collection)
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
