package io.github.fabiann1809.reader.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.GenerateQuiz
import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    /** The AI is writing the questions. */
    data object Loading : QuizUiState

    data class Failed(val error: Throwable) : QuizUiState

    /** Question [index] of [quiz]; [chosen] is the option picked, which reveals the answer. */
    data class Answering(
        val quiz: Quiz,
        val index: Int = 0,
        val chosen: Int? = null,
        val correctSoFar: Int = 0,
    ) : QuizUiState {
        val question: QuizQuestion get() = quiz.questions[index]
        val isLast: Boolean get() = index == quiz.questions.lastIndex
    }

    /** Every question answered; T15.4 turns this into the full result. */
    data class Finished(val correct: Int, val total: Int) : QuizUiState
}

/** A quiz of [questionCount] questions about [source] (T15.3): one question at a time, answer shown at once. */
class QuizViewModel(
    private val source: String,
    private val questionCount: Int,
    private val generateQuiz: GenerateQuiz,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        if (_uiState.value is QuizUiState.Failed) load()
    }

    /** Picks option [option] of the current question; the first pick counts, later taps do nothing. */
    fun choose(option: Int) {
        val state = _uiState.value as? QuizUiState.Answering ?: return
        if (state.chosen != null) return
        val right = option == state.question.correctIndex
        _uiState.value = state.copy(chosen = option, correctSoFar = state.correctSoFar + if (right) 1 else 0)
    }

    /** "Siguiente", or the end after the last question. */
    fun next() {
        val state = _uiState.value as? QuizUiState.Answering ?: return
        if (state.chosen == null) return
        _uiState.value = if (state.isLast) {
            QuizUiState.Finished(correct = state.correctSoFar, total = state.quiz.questions.size)
        } else {
            state.copy(index = state.index + 1, chosen = null)
        }
    }

    private fun load() {
        _uiState.value = QuizUiState.Loading
        viewModelScope.launch {
            _uiState.value = generateQuiz(source, questionCount).fold(
                onSuccess = { QuizUiState.Answering(it) },
                onFailure = { QuizUiState.Failed(it) },
            )
        }
    }
}
