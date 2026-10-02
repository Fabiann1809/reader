package io.github.fabiann1809.reader.data.flashcard

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class SpacedRepetitionTest {

    private val now = 1_000_000L
    private val newCard = Flashcard(bookId = 1, front = "¿?", back = "!", nextReviewAt = 0)
    private val learnedCard = newCard.copy(repetitions = 3, intervalDays = 10, easeFactor = 2.5)

    @Test
    fun aNewCardAnsweredGoodComesBackTomorrowThenInThreeDaysThenGrowsByItsEase() {
        val first = newCard.reviewed(ReviewGrade.GOOD, now)
        assertEquals(now + 1.days.inWholeMilliseconds, first.nextReviewAt)
        assertEquals(1, first.repetitions)

        val second = first.reviewed(ReviewGrade.GOOD, now)
        assertEquals(3, second.intervalDays)

        val third = second.reviewed(ReviewGrade.GOOD, now)
        // 3 days × 2.5 ease, rounded.
        assertEquals(8, third.intervalDays)
        assertEquals(now + 8.days.inWholeMilliseconds, third.nextReviewAt)
        assertEquals(2.5, third.easeFactor, 0.0)
    }

    @Test
    fun againStartsOverInAMinuteAndLowersTheEase() {
        val forgotten = learnedCard.reviewed(ReviewGrade.AGAIN, now)

        assertEquals(now + 1.minutes.inWholeMilliseconds, forgotten.nextReviewAt)
        assertEquals(0, forgotten.repetitions)
        assertEquals(0, forgotten.intervalDays)
        assertEquals(2.3, forgotten.easeFactor, 1e-9)
    }

    @Test
    fun hardWhileLearningRepeatsInTenMinutesAndOnceLearnedGrowsALittle() {
        val learning = newCard.reviewed(ReviewGrade.HARD, now)
        assertEquals(now + 10.minutes.inWholeMilliseconds, learning.nextReviewAt)
        assertEquals(0, learning.repetitions)

        val learned = learnedCard.reviewed(ReviewGrade.HARD, now)
        assertEquals(12, learned.intervalDays)
        assertEquals(4, learned.repetitions)
        assertEquals(2.35, learned.easeFactor, 1e-9)
    }

    @Test
    fun easyGoesFurtherThanGoodAndRaisesTheEase() {
        val good = learnedCard.outcomeOf(ReviewGrade.GOOD)
        val easy = learnedCard.outcomeOf(ReviewGrade.EASY)

        assertEquals(25, good.intervalDays)
        assertEquals(33, easy.intervalDays)
        assertEquals(2.65, easy.easeFactor, 1e-9)
        assertEquals(4, newCard.outcomeOf(ReviewGrade.EASY).intervalDays)
    }

    @Test
    fun theEaseNeverDropsBelowTheFloor() {
        var card = learnedCard.copy(easeFactor = 1.35)

        repeat(3) { card = card.reviewed(ReviewGrade.AGAIN, now) }

        assertEquals(1.3, card.easeFactor, 1e-9)
    }
}
