package io.github.fabiann1809.reader.ui.reviewsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.data.flashcard.reviewed
import io.github.fabiann1809.reader.ui.review.DueCard
import io.github.fabiann1809.reader.util.endOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ReviewSessionUiState {
    data object Loading : ReviewSessionUiState

    /** Card [index] of [cards] is shown; [flipped] once its back is visible and it can be graded. */
    data class Reviewing(val cards: List<DueCard>, val index: Int = 0, val flipped: Boolean = false) : ReviewSessionUiState {
        val current: DueCard get() = cards[index]
    }

    /** Every card was answered; [correct] counts the "Bien" and "Fácil" ones (for T14.5's summary). */
    data class Finished(val reviewed: Int, val correct: Int) : ReviewSessionUiState
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
            ReviewSessionUiState.Finished(reviewed = state.cards.size, correct = correct)
        }
        viewModelScope.launch { flashcardRepository.updateFlashcard(state.current.card.reviewed(grade, now())) }
    }
}
