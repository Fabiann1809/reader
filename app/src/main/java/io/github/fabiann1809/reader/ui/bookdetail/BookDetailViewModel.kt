package io.github.fabiann1809.reader.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.BookStatus
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

    data class Success(val book: Book, val notes: List<Note>) : BookDetailUiState

    // The book no longer exists (e.g. it was deleted).
    data object NotFound : BookDetailUiState
}

class BookDetailViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
    noteRepository: NoteRepository,
) : ViewModel() {

    // Notes are already sorted newest first by the query.
    val uiState: StateFlow<BookDetailUiState> =
        combine(bookRepository.observeBook(bookId), noteRepository.observeNotes(bookId)) { book, notes ->
            if (book == null) BookDetailUiState.NotFound else BookDetailUiState.Success(book, notes)
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

    /** Deletes the book; its notes are removed by the database cascade. */
    fun deleteBook() {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.deleteBook(book)
            _isDeleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
