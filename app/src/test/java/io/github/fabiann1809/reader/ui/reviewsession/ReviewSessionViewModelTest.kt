package io.github.fabiann1809.reader.ui.reviewsession

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeFlashcardRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class ReviewSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_790_900_000_000L
    private val books = FakeBookRepository(listOf(Book(id = 1, title = "Cosmos", author = "")))
    private val cards = FakeFlashcardRepository(
        listOf(
            Flashcard(id = 1, bookId = 1, front = "¿Uno?", back = "1", nextReviewAt = now - 2),
            Flashcard(id = 2, bookId = 1, front = "¿Dos?", back = "2", nextReviewAt = now - 1),
        ),
    )

    private fun viewModel() = ReviewSessionViewModel(cards, books, now = { now })

    private fun reviewing(viewModel: ReviewSessionViewModel) = viewModel.uiState.value as ReviewSessionUiState.Reviewing

    @Test
    fun aCardIsGradedOnlyAfterFlippingAndIsRescheduled() {
        val viewModel = viewModel()
        assertEquals("¿Uno?", reviewing(viewModel).current.card.front)
        assertEquals("Cosmos", reviewing(viewModel).current.bookTitle)

        viewModel.grade(ReviewGrade.GOOD)
        assertEquals(0, reviewing(viewModel).index)

        viewModel.flip()
        assertTrue(reviewing(viewModel).flipped)
        viewModel.grade(ReviewGrade.GOOD)

        assertEquals(1, reviewing(viewModel).index)
        assertFalse(reviewing(viewModel).flipped)
        val first = cards.currentCards.first { it.id == 1L }
        assertEquals(now + 1.days.inWholeMilliseconds, first.nextReviewAt)
        assertEquals(1, first.repetitions)
    }

    @Test
    fun theSessionEndsCountingTheCorrectAnswers() {
        val viewModel = viewModel()
        viewModel.flip()
        viewModel.grade(ReviewGrade.AGAIN)
        viewModel.flip()
        viewModel.grade(ReviewGrade.EASY)

        // The next review is the card answered "Otra vez": back in a minute.
        assertEquals(
            ReviewSessionUiState.Finished(
                reviewed = 2,
                correct = 1,
                nextReviewAt = now + 1.minutes.inWholeMilliseconds,
                quizSource = "¿Uno? — 1\n¿Dos? — 2",
                quizBookId = 1,
            ),
            viewModel.uiState.value,
        )
        assertEquals(now + 1.minutes.inWholeMilliseconds, cards.currentCards.first { it.id == 1L }.nextReviewAt)
        assertEquals(now + 4.days.inWholeMilliseconds, cards.currentCards.first { it.id == 2L }.nextReviewAt)
    }

    @Test
    fun withNothingDueTheSessionIsAlreadyOver() {
        val viewModel = ReviewSessionViewModel(FakeFlashcardRepository(), books, now = { now })

        assertEquals(ReviewSessionUiState.Finished(reviewed = 0, correct = 0), viewModel.uiState.value)
    }
}
