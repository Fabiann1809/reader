package io.github.fabiann1809.reader.data.flashcard

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/** The four answers of a review (design: Otra vez · Difícil · Bien · Fácil). */
enum class ReviewGrade { AGAIN, HARD, GOOD, EASY }

/** A card's spaced repetition values after one answer: when it comes back and how its ease changed. */
data class ReviewOutcome(
    val wait: Duration,
    val intervalDays: Int,
    val repetitions: Int,
    val easeFactor: Double,
)

/**
 * Simplified SM-2 (T14.4). A new or forgotten card (no correct answers in a row) is relearned within
 * minutes; once learned, its interval grows by its ease: 1 day, 3 days, then interval × ease. "Otra
 * vez" starts over and lowers the ease, "Difícil" grows the interval only a little, "Fácil" a lot.
 */
fun Flashcard.outcomeOf(grade: ReviewGrade): ReviewOutcome = when (grade) {
    ReviewGrade.AGAIN -> ReviewOutcome(
        wait = AGAIN_WAIT,
        intervalDays = 0,
        repetitions = 0,
        easeFactor = adjustedEase(-AGAIN_EASE_DROP),
    )
    ReviewGrade.HARD -> if (repetitions == 0) {
        // Still being learned: once more in a few minutes, without counting it as learned.
        ReviewOutcome(HARD_LEARNING_WAIT, intervalDays = 0, repetitions = 0, easeFactor = adjustedEase(-HARD_EASE_DROP))
    } else {
        learned(max(1, (intervalDays * HARD_GROWTH).roundToInt()), adjustedEase(-HARD_EASE_DROP))
    }
    ReviewGrade.GOOD -> learned(goodInterval(), easeFactor)
    ReviewGrade.EASY -> learned(easyInterval(), adjustedEase(EASY_EASE_GAIN))
}

/** The card after being answered with [grade] at [now] (epoch milliseconds). */
fun Flashcard.reviewed(grade: ReviewGrade, now: Long): Flashcard {
    val outcome = outcomeOf(grade)
    return copy(
        nextReviewAt = now + outcome.wait.inWholeMilliseconds,
        intervalDays = outcome.intervalDays,
        repetitions = outcome.repetitions,
        easeFactor = outcome.easeFactor,
    )
}

private fun Flashcard.goodInterval(): Int = when (repetitions) {
    0 -> FIRST_INTERVAL_DAYS
    1 -> SECOND_INTERVAL_DAYS
    else -> max(intervalDays + 1, (intervalDays * easeFactor).roundToInt())
}

// Always further than "Bien": a new card known at once can wait a few days.
private fun Flashcard.easyInterval(): Int =
    if (repetitions == 0) EASY_FIRST_INTERVAL_DAYS else max(goodInterval() + 1, (goodInterval() * EASY_BONUS).roundToInt())

private fun Flashcard.learned(days: Int, ease: Double) =
    ReviewOutcome(wait = days.days, intervalDays = days, repetitions = repetitions + 1, easeFactor = ease)

private fun Flashcard.adjustedEase(change: Double): Double = max(MIN_EASE, easeFactor + change)

private val AGAIN_WAIT = 1.minutes
private val HARD_LEARNING_WAIT = 10.minutes
private const val FIRST_INTERVAL_DAYS = 1
private const val SECOND_INTERVAL_DAYS = 3
private const val EASY_FIRST_INTERVAL_DAYS = 4
private const val HARD_GROWTH = 1.2
private const val EASY_BONUS = 1.3
private const val AGAIN_EASE_DROP = 0.2
private const val HARD_EASE_DROP = 0.15
private const val EASY_EASE_GAIN = 0.15

// SM-2's floor: below it, a hard card would come back almost every day forever.
private const val MIN_EASE = 1.3
