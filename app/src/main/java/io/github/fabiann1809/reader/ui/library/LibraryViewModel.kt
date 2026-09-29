package io.github.fabiann1809.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
)

class LibraryViewModel(bookRepository: BookRepository) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = bookRepository.observeBooks()
        .map { books -> LibraryUiState(books = books, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            // Keeps the query alive briefly across configuration changes (e.g. rotation).
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = LibraryUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
