package io.github.fabiann1809.reader.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState

    data class Success(val book: Book) : BookDetailUiState

    // The book no longer exists (e.g. it was deleted).
    data object NotFound : BookDetailUiState
}

class BookDetailViewModel(
    bookId: Long,
    bookRepository: BookRepository,
) : ViewModel() {

    val uiState: StateFlow<BookDetailUiState> = bookRepository.observeBook(bookId)
        .map { book -> if (book == null) BookDetailUiState.NotFound else BookDetailUiState.Success(book) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BookDetailUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
