package io.github.fabiann1809.reader.ui.noteeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.note.NoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val content: String = "",
    // Kept as text so the field can be empty; converted to Int on save.
    val page: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isPageValid: Boolean
        get() = page.isEmpty() || (page.toIntOrNull() ?: 0) > 0

    val canSave: Boolean
        get() = content.isNotBlank() && isPageValid && !isSaving
}

class NoteEditorViewModel(
    private val bookId: Long,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }

    fun onPageChange(page: String) {
        if (page.all(Char::isDigit) && page.length <= MAX_PAGE_DIGITS) {
            _uiState.update { it.copy(page = page) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            noteRepository.addNote(
                Note(
                    bookId = bookId,
                    page = state.page.toIntOrNull(),
                    content = state.content.trim(),
                    type = NoteType.MANUAL,
                ),
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private companion object {
        const val MAX_PAGE_DIGITS = 5
    }
}
