package io.github.fabiann1809.reader.ui.reviewsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.data.flashcard.reviewed
import io.github.fabiann1809.reader.ui.review.DueCard
import io.github.fabiann1809.reader.util.endOfDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

sealed interface ReviewSessionUiState {
    data object Loading : ReviewSessionUiState

    /** Card [index] of [cards] is shown; [flipped] once its back is visible and it can be graded. */
    data class Reviewing(val cards: List<DueCard>, val index: Int = 0, val flipped: Boolean = false) : ReviewSessionUiState {
        val current: DueCard get() = cards[index]
    }

    /**
     * Every card was answered (T14.5's summary): [correct] counts the "Bien" and "Fácil" ones, and
     * [nextReviewAt] is when the next card is due (null while it is being looked up, or without cards).
     */
    data class Finished(
        val reviewed: Int,
        val correct: Int,
        val nextReviewAt: Long? = null,
        // The reviewed cards as text, for "Ponme a prueba" (T15.3).
        val quizSource: String = "",
    ) : ReviewSessionUiState
}

/**
 * A review session (T14.4): today's cards, one at a time. Tapping flips the card; each answer
 * reschedules it with the simplified SM-2 and moves on.
 */
class ReviewSessionViewModel(
    private val flashcardRepository: FlashcardRepository,
    private val bookRepository: BookRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewSessionUiState>(ReviewSessionUiState.Loading)
    val uiState: StateFlow<ReviewSessionUiState> = _uiState.asStateFlow()

    private var correct = 0

    // The answers being saved: the summary waits for them before looking up the next review.
    private val saves = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            // Taken once: a card answered "Otra vez" comes back later today, not again in this session.
            val cards = flashcardRepository.observeDue(endOfDay(now())).first()
            val titles = bookRepository.observeBooks().first().associate { it.id to it.title }
            _uiState.value = if (cards.isEmpty()) {
                ReviewSessionUiState.Finished(reviewed = 0, correct = 0)
            } else {
                ReviewSessionUiState.Reviewing(cards.map { DueCard(it, titles[it.bookId].orEmpty()) })
            }
        }
    }

    fun flip() {
        val state = _uiState.value as? ReviewSessionUiState.Reviewing ?: return
        _uiState.value = state.copy(flipped = !state.flipped)
    }

    fun grade(grade: ReviewGrade) {
        val state = _uiState.value as? ReviewSessionUiState.Reviewing ?: return
        if (!state.flipped) return
        if (grade == ReviewGrade.GOOD || grade == ReviewGrade.EASY) correct++
        val next = state.index + 1
        // Moved on at once so a quick second tap can't grade the same card twice.
        _uiState.value = if (next < state.cards.size) {
            state.copy(index = next, flipped = false)
        } else {
            ReviewSessionUiState.Finished(reviewed = state.cards.size, correct = correct, quizSource = state.cards.asQuizSource())
        }
        saves += viewModelScope.launch { flashcardRepository.updateFlashcard(state.current.card.reviewed(grade, now())) }
        if (next == state.cards.size) showNextReview()
    }

    private fun List<DueCard>.asQuizSource(): String =
        joinToString(separator = "\n") { "${it.card.front} — ${it.card.back}" }

    private fun showNextReview() {
        viewModelScope.launch {
            saves.joinAll()
            val nextReviewAt = flashcardRepository.nextReviewAt()
            _uiState.update { if (it is ReviewSessionUiState.Finished) it.copy(nextReviewAt = nextReviewAt) else it }
        }
    }
}
