package io.github.fabiann1809.reader.ui.interpretation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.AnalyzeInterpretation
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AnalysisState {
    data object Idle : AnalysisState

    data object Loading : AnalysisState

    data class Done(val analysis: InterpretationAnalysis) : AnalysisState

    data class Failed(val error: Throwable) : AnalysisState
}

data class InterpretationUiState(
    val sourceText: String,
    val ownWords: String = "",
    val analysis: AnalysisState = AnalysisState.Idle,
) {
    val canAnalyze: Boolean get() = ownWords.isNotBlank() && analysis != AnalysisState.Loading
}

/** "Ahora tú" (T15.1): the reader writes what they understood of [sourceText] and the AI reviews it. */
class InterpretationViewModel(
    sourceText: String,
    private val analyzeInterpretation: AnalyzeInterpretation,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterpretationUiState(sourceText))
    val uiState: StateFlow<InterpretationUiState> = _uiState.asStateFlow()

    fun onOwnWordsChange(text: String) = _uiState.update { it.copy(ownWords = text) }

    /** "Analizar"; it can be asked again after rewriting the interpretation. */
    fun analyze() {
        val state = _uiState.value
        if (!state.canAnalyze) return
        _uiState.update { it.copy(analysis = AnalysisState.Loading) }
        viewModelScope.launch {
            val result = analyzeInterpretation(state.sourceText, state.ownWords)
            _uiState.update {
                it.copy(analysis = result.fold({ analysis -> AnalysisState.Done(analysis) }, { error -> AnalysisState.Failed(error) }))
            }
        }
    }
}
