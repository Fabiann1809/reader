package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    /** The book is open in the [ReaderSession]; the navigator takes it from there. */
    data class Ready(val bookId: Long) : ReaderUiState

    data class CannotOpen(val problem: OpenProblem) : ReaderUiState
}

class ReaderViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
    private val session: ReaderSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId)
            val problem = if (book == null) OpenProblem.NO_FILE else session.open(book)
            _uiState.value = if (problem == null) ReaderUiState.Ready(bookId) else ReaderUiState.CannotOpen(problem)
        }
    }

    // Leaving the reader frees the book; a rotation keeps this ViewModel, so the book stays open.
    override fun onCleared() {
        session.close(bookId)
    }
}
