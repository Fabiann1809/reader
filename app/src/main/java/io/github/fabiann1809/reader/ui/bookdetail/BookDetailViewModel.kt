package io.github.fabiann1809.reader.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState

    data class Success(
        val book: Book,
        val notes: List<Note>,
        /** User collections, for "Añadir a colección". */
        val collections: List<Collection> = emptyList(),
        /** Ids of the user collections that already contain the book. */
        val collectionIds: Set<Long> = emptySet(),
    ) : BookDetailUiState

    // The book no longer exists (e.g. it was deleted).
    data object NotFound : BookDetailUiState
}

class BookDetailViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
    noteRepository: NoteRepository,
    collectionRepository: CollectionRepository,
    private val organizer: BookOrganizer,
) : ViewModel() {

    // Notes are already sorted newest first by the query.
    val uiState: StateFlow<BookDetailUiState> =
        combine(
            bookRepository.observeBook(bookId),
            noteRepository.observeNotes(bookId),
            collectionRepository.observeCollections(),
            collectionRepository.observeCollectionIdsOf(bookId),
        ) { book, notes, collections, collectionIds ->
            if (book == null) {
                BookDetailUiState.NotFound
            } else {
                BookDetailUiState.Success(book, notes, collections, collectionIds.toSet())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BookDetailUiState.Loading,
        )

    private val _isDeleted = MutableStateFlow(false)

    /** Becomes true once the book is deleted, so the screen can close itself. */
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    fun updateProgress(currentPage: Int, status: BookStatus) {
        viewModelScope.launch {
            // Read the stored book rather than the UI state so a stale screen can't overwrite newer data.
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.updateBook(book.copy(currentPage = currentPage, status = status))
        }
    }

    fun setFavorite(isFavorite: Boolean) {
        viewModelScope.launch { organizer.setFavorite(listOf(bookId), isFavorite) }
    }

    fun setInCollection(collectionId: Long, isIncluded: Boolean) {
        viewModelScope.launch { organizer.setInCollection(bookId, collectionId, isIncluded) }
    }

    /** Creates a collection that already holds this book. */
    fun createCollectionWithBook(name: String) {
        viewModelScope.launch { organizer.createCollectionWith(listOf(bookId), name) }
    }

    /** Deletes the book with its notes and files. */
    fun deleteBook() {
        viewModelScope.launch {
            organizer.delete(listOf(bookId))
            _isDeleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
