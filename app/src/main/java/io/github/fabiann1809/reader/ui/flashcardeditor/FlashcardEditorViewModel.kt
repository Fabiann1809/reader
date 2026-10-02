package io.github.fabiann1809.reader.ui.flashcardeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.MakeFlashcard
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.data.note.NoteTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlashcardEditorUiState(
    val front: String = "",
    val back: String = "",
    // Kept as text so the field can be empty; converted to Int on save.
    val page: String = "",
    // The note, explanation or selection the card comes from; null for a card written from scratch.
    val source: String? = null,
    val isSuggesting: Boolean = false,
    // The fields hold the AI's proposal: it carries the "Generado con IA" label.
    val isAiSuggested: Boolean = false,
    val suggestionError: Throwable? = null,
    val isSaving: Boolean = false,
    // Closes the editor.
    val isSaved: Boolean = false,
) {
    val isPageValid: Boolean
        get() = page.isEmpty() || (page.toIntOrNull() ?: 0) > 0

    val canSuggest: Boolean get() = source != null && !isSuggesting

    val canSave: Boolean
        get() = front.isNotBlank() && back.isNotBlank() && isPageValid && !isSaving && !isSuggesting
}

/**
 * Writes a new review card of [bookId] (T14.2): from scratch, or from a [source] text that the AI
 * can turn into a proposal with "Sugerir con IA" (a button, so no quota is spent unless asked).
 */
class FlashcardEditorViewModel(
    private val bookId: Long,
    private val flashcardRepository: FlashcardRepository,
    private val makeFlashcard: MakeFlashcard,
    source: String? = null,
    page: Int? = null,
    private val tag: NoteTag? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FlashcardEditorUiState(source = source?.takeIf { it.isNotBlank() }, page = page?.toString().orEmpty()),
    )
    val uiState: StateFlow<FlashcardEditorUiState> = _uiState.asStateFlow()

    fun onFrontChange(front: String) = _uiState.update { it.copy(front = front) }

    fun onBackChange(back: String) = _uiState.update { it.copy(back = back) }

    fun onPageChange(page: String) {
        if (page.all(Char::isDigit) && page.length <= MAX_PAGE_DIGITS) _uiState.update { it.copy(page = page) }
    }

    /** "Sugerir con IA": fills the front and back with the AI's proposal, ready to edit. */
    fun suggest() {
        val state = _uiState.value
        val source = state.source ?: return
        if (!state.canSuggest) return
        _uiState.update { it.copy(isSuggesting = true, suggestionError = null) }
        viewModelScope.launch {
            makeFlashcard(source).fold(
                onSuccess = { draft ->
                    _uiState.update {
                        it.copy(front = draft.front, back = draft.back, isSuggesting = false, isAiSuggested = true)
                    }
                },
                onFailure = { error -> _uiState.update { it.copy(isSuggesting = false, suggestionError = error) } },
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            flashcardRepository.addFlashcard(
                Flashcard(
                    bookId = bookId,
                    page = state.page.toIntOrNull(),
                    front = state.front.trim(),
                    back = state.back.trim(),
                    tag = tag,
                ),
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private companion object {
        const val MAX_PAGE_DIGITS = 5
    }
}
