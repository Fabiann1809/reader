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

/** Room ids start at 1, so 0 means "create a new note". */
const val NEW_NOTE_ID = 0L

data class NoteEditorUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val content: String = "",
    // Kept as text so the field can be empty; converted to Int on save.
    val page: String = "",
    val isSaving: Boolean = false,
    // Either flag closes the editor.
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    // The passage a note from the reader is about (T11.13), shown above the note.
    val quote: String? = null,
) {
    val isPageValid: Boolean
        get() = page.isEmpty() || (page.toIntOrNull() ?: 0) > 0

    val canSave: Boolean
        get() = content.isNotBlank() && isPageValid && !isSaving && !isLoading
}

class NoteEditorViewModel(
    private val bookId: Long,
    private val noteId: Long,
    private val noteRepository: NoteRepository,
    // A new note written on a passage in the reader: its text and place (T11.13).
    private val sourceText: String? = null,
    private val location: String? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            isEditing = noteId != NEW_NOTE_ID,
            isLoading = noteId != NEW_NOTE_ID,
            quote = sourceText.takeIf { location != null },
        ),
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    // The note being edited; keeps fields the editor doesn't show (type, sourceText, createdAt).
    private var originalNote: Note? = null

    init {
        if (noteId != NEW_NOTE_ID) loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            val note = noteRepository.getNote(noteId)
            if (note == null) {
                // Deleted elsewhere: nothing to edit, so close the editor.
                _uiState.update { it.copy(isLoading = false, isDeleted = true) }
                return@launch
            }
            originalNote = note
            _uiState.update {
                it.copy(
                    isLoading = false,
                    content = note.content,
                    page = note.page?.toString().orEmpty(),
                    quote = note.sourceText.takeIf { note.location != null },
                )
            }
        }
    }

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
            val content = state.content.trim()
            val page = state.page.toIntOrNull()
            val original = originalNote
            if (original == null) {
                noteRepository.addNote(
                    Note(
                        bookId = bookId,
                        page = page,
                        sourceText = sourceText,
                        content = content,
                        type = NoteType.MANUAL,
                        location = location,
                    ),
                )
            } else {
                noteRepository.updateNote(original.copy(content = content, page = page))
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun delete() {
        val note = originalNote ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    private companion object {
        const val MAX_PAGE_DIGITS = 5
    }
}
