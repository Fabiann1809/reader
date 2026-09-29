package io.github.fabiann1809.reader.ui.addbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookUiState(
    val title: String = "",
    val author: String = "",
    // Kept as text so the field can be empty; converted to Int on save.
    val totalPages: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isTotalPagesValid: Boolean
        get() = totalPages.isEmpty() || (totalPages.toIntOrNull() ?: 0) > 0

    val canSave: Boolean
        get() = title.isNotBlank() && author.isNotBlank() && isTotalPagesValid && !isSaving
}

class AddBookViewModel(private val bookRepository: BookRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }

    fun onAuthorChange(author: String) = _uiState.update { it.copy(author = author) }

    fun onTotalPagesChange(totalPages: String) {
        // Only digits, and a sane upper bound to avoid Int overflow.
        if (totalPages.all(Char::isDigit) && totalPages.length <= MAX_PAGES_DIGITS) {
            _uiState.update { it.copy(totalPages = totalPages) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            bookRepository.addBook(
                Book(
                    title = state.title.trim(),
                    author = state.author.trim(),
                    totalPages = state.totalPages.toIntOrNull(),
                ),
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private companion object {
        const val MAX_PAGES_DIGITS = 5
    }
}
