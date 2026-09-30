package io.github.fabiann1809.reader.ui.explanation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.ExplainText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExplanationState {
    data object Loading : ExplanationState

    data class Success(val explanation: String) : ExplanationState

    data class Failed(val error: Throwable) : ExplanationState
}

data class ExplanationUiState(
    val sourceText: String,
    val explanation: ExplanationState = ExplanationState.Loading,
)

class ExplanationViewModel(
    private val sourceText: String,
    private val explainText: ExplainText,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplanationUiState(sourceText))
    val uiState: StateFlow<ExplanationUiState> = _uiState.asStateFlow()

    init {
        explain()
    }

    fun retry() = explain()

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
