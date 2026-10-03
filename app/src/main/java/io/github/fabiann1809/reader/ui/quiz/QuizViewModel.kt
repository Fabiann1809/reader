package io.github.fabiann1809.reader.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.GenerateQuiz
import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizQuestion
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        val answers: List<Int> = emptyList(),
    ) : QuizUiState {
        val question: QuizQuestion get() = quiz.questions[index]
        val isLast: Boolean get() = index == quiz.questions.lastIndex
    }

    /** The result (T15.4); [cardsCreated] once the missed questions became cards. */
    data class Finished(val result: QuizResult, val cardsCreated: Boolean = false) : QuizUiState
}

/**
 * A quiz of [questionCount] questions about [source] (T15.3): one question at a time, the answer
 * shown at once, and a result whose missed questions can become cards of [bookId] (T15.4).
 */
class QuizViewModel(
    private val bookId: Long,
    private val source: String,
    private val questionCount: Int,
    private val generateQuiz: GenerateQuiz,
    private val flashcardRepository: FlashcardRepository,
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
        _uiState.value = state.copy(chosen = option, answers = state.answers + option)
    }

    /** "Siguiente", or the result after the last question. */
    fun next() {
        val state = _uiState.value as? QuizUiState.Answering ?: return
        if (state.chosen == null) return
        _uiState.value = if (state.isLast) {
            QuizUiState.Finished(QuizResult.of(state.quiz, state.answers))
        } else {
            state.copy(index = state.index + 1, chosen = null)
        }
    }

    /** "Crear fichas de lo que falló": each missed question becomes a card with its right answer. */
    fun createCardsFromMistakes() {
        val state = _uiState.value as? QuizUiState.Finished ?: return
        if (state.cardsCreated || state.result.missed.isEmpty()) return
        _uiState.value = state.copy(cardsCreated = true)
        viewModelScope.launch {
            state.result.missed.forEach { question ->
                flashcardRepository.addFlashcard(
                    Flashcard(bookId = bookId, front = question.question, back = question.answerForCard()),
                )
            }
        }
    }

    // The answer ends with its own period when it has one, so "sombra." doesn't become "sombra..".
    private fun QuizQuestion.answerForCard(): String = "${options[correctIndex].trimEnd('.')}. $explanation"

    private fun load() {
        _uiState.value = QuizUiState.Loading
        viewModelScope.launch {
            val result = generateQuiz(source, questionCount)
            _uiState.update {
                result.fold(onSuccess = { quiz -> QuizUiState.Answering(quiz) }, onFailure = { error -> QuizUiState.Failed(error) })
            }
        }
    }
}
