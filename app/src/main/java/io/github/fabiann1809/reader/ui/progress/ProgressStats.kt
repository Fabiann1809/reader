package io.github.fabiann1809.reader.ui.progress

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.prefs.GoalUnit
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.data.session.ReadingSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Minutes read on one day of this week, for the weekly chart. */
data class DayMinutes(val day: DayOfWeek, val minutes: Int)

/** What the Progreso tab shows (lámina 1i). */
data class ProgressStats(
    val todayMinutes: Int,
    val todayPages: Int,
    val goal: ReadingGoal,
    val streakDays: Int,
    val week: List<DayMinutes>,
    // Titles of the books finished this year, oldest first, for the little spines on the card.
    val finishedTitles: List<String>,
    val year: Int,
) {
    val finishedThisYear: Int get() = finishedTitles.size

    val weekMinutes: Int get() = week.sumOf { it.minutes }

    /** The average of the days read this week ("42 min de media"); 0 if none yet. */
    val weekAverageMinutes: Int
        get() = week.filter { it.minutes > 0 }.let { days -> if (days.isEmpty()) 0 else weekMinutes / days.size }

    /** Today's minutes or pages, whichever the goal counts. */
    val todayAmount: Int get() = if (goal.unit == GoalUnit.PAGES) todayPages else todayMinutes

    /** How much of today's goal is done, from 0 to 1. */
    val goalProgress: Float get() = if (goal.amount <= 0) 0f else (todayAmount.toFloat() / goal.amount).coerceIn(0f, 1f)
}

/**
 * The stats at [now] in [zone] from the reading [sessions] (of at least the last year, for the streak)
 * and the [books]. Weeks start on Monday, as in Spanish calendars.
 */
fun progressStats(
    sessions: List<ReadingSession>,
    books: List<Book>,
    goal: ReadingGoal,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): ProgressStats {
    val today = now.toDate(zone)
    val minutesByDay = sessions.groupBy { it.startedAt.toDate(zone) }
        .mapValues { (_, daySessions) -> (daySessions.sumOf { it.durationMillis } / MILLIS_PER_MINUTE).toInt() }
    // A day counts for the streak if there was any reading: a paper book's pages have no duration.
    val daysRead = sessions.map { it.startedAt.toDate(zone) }.toSet()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return ProgressStats(
        todayMinutes = minutesByDay[today] ?: 0,
        todayPages = sessions.filter { it.startedAt.toDate(zone) == today }.sumOf { it.pagesRead },
        goal = goal,
        streakDays = streak(daysRead, today),
        week = (0L..6L).map { offset ->
            val day = monday.plusDays(offset)
            DayMinutes(day.dayOfWeek, minutesByDay[day] ?: 0)
        },
        finishedTitles = books
            .filter { book -> book.status == BookStatus.FINISHED && book.finishedAt?.toDate(zone)?.year == today.year }
            .sortedBy { it.finishedAt }
            .map { it.title },
        year = today.year,
    )
}

/** Days in a row with reading, up to today; a streak still counts today if yesterday was read. */
private fun streak(daysRead: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in daysRead) today else today.minusDays(1)
    var count = 0
    while (day in daysRead) {
        count++
        day = day.minusDays(1)
    }
    return count
}

private fun Long.toDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

private const val MILLIS_PER_MINUTE = 60_000L
