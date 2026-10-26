package io.github.fabiann1809.reader.ui.addbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.CoverStore
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
    // A picture the user chose; null keeps the cover generated from the title.
    val coverPath: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isTotalPagesValid: Boolean
        get() = totalPages.isEmpty() || (totalPages.toIntOrNull() ?: 0) > 0

    val canSave: Boolean
        get() = title.isNotBlank() && author.isNotBlank() && isTotalPagesValid && !isSaving
}

class AddBookViewModel(
    private val bookRepository: BookRepository,
    private val coverStore: CoverStore,
) : ViewModel() {

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

    fun onCoverPicked(uri: String) {
        viewModelScope.launch {
            val path = coverStore.save(uri) ?: return@launch
            coverStore.delete(_uiState.value.coverPath)
            _uiState.update { it.copy(coverPath = path) }
        }
    }

    fun onCoverRemoved() {
        coverStore.delete(_uiState.value.coverPath)
        _uiState.update { it.copy(coverPath = null) }
    }

    // A cover picked but never saved with a book would stay in storage for nothing.
    override fun onCleared() {
        if (!_uiState.value.isSaved) coverStore.delete(_uiState.value.coverPath)
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
                    coverPath = state.coverPath,
                ),
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private companion object {
        const val MAX_PAGES_DIGITS = 5
    }
}
