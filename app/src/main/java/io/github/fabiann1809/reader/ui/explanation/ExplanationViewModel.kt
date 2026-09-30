package io.github.fabiann1809.reader.ui.explanation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.ExplainText
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.note.NoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExplanationState {
    data object Loading : ExplanationState

    data class Success(val explanation: Explanation) : ExplanationState

    data class Failed(val error: Throwable) : ExplanationState
}

enum class SaveState { NOT_SAVED, SAVING, SAVED }

data class ExplanationUiState(
    val sourceText: String,
    val explanation: ExplanationState = ExplanationState.Loading,
    val saveState: SaveState = SaveState.NOT_SAVED,
) {
    val canSave: Boolean get() = explanation is ExplanationState.Success && saveState == SaveState.NOT_SAVED
}

class ExplanationViewModel(
    private val bookId: Long,
    private val sourceText: String,
    private val explainText: ExplainText,
    private val noteRepository: NoteRepository,
    private val labels: ExplanationLabels,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplanationUiState(sourceText))
    val uiState: StateFlow<ExplanationUiState> = _uiState.asStateFlow()

    init {
        explain()
    }

    fun retry() = explain()

    /** Saves the explanation as a note of the book, keeping the original fragment next to it. */
    fun saveAsNote() {
        val state = _uiState.value
        if (!state.canSave) return
        val explanation = (state.explanation as ExplanationState.Success).explanation
        _uiState.update { it.copy(saveState = SaveState.SAVING) }
        viewModelScope.launch {
            noteRepository.addNote(
                Note(
                    bookId = bookId,
                    sourceText = sourceText,
                    content = explanation.toPlainText(labels),
                    type = NoteType.EXPLANATION,
                ),
            )
            _uiState.update { it.copy(saveState = SaveState.SAVED) }
        }
    }

    private fun explain() {
        _uiState.update { it.copy(explanation = ExplanationState.Loading) }
        viewModelScope.launch {
            val explanation = explainText(sourceText).fold(
                onSuccess = { ExplanationState.Success(it) },
                onFailure = { ExplanationState.Failed(it) },
            )
            _uiState.update { it.copy(explanation = explanation) }
        }
    }
}
