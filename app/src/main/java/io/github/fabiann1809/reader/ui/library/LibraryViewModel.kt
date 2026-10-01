package io.github.fabiann1809.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.book.arrangedBy
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
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    bookRepository: BookRepository,
    private val collectionRepository: CollectionRepository,
    private val preferences: AppPreferences,
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

    val uiState: StateFlow<LibraryUiState> = combine(
        filter.flatMapLatest { (filter, collection) ->
            collectionRepository.observeBooks(filter).map { books -> Triple(filter, collection, books) }
        },
        collectionRepository.observeCollections(),
        bookRepository.observeBooks().map { it.isEmpty() },
        query,
        // combine() takes at most five typed flows, so the two display preferences travel together.
        combine(preferences.libraryArrangement, preferences.libraryLayout, ::Pair),
    ) { (filter, collection, books), collections, libraryIsEmpty, query, (arrangement, layout) ->
        LibraryUiState(
            books = books.filter { it.matchesSearch(query) }.arrangedBy(arrangement),
            isLoading = false,
            filter = filter,
            currentCollection = collection,
            collections = collections,
            libraryIsEmpty = libraryIsEmpty,
            query = query,
            arrangement = arrangement,
            layout = layout,
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
