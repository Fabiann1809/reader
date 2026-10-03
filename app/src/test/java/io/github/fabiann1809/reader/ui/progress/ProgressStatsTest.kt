package io.github.fabiann1809.reader.ui.progress

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.session.ReadingSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class ProgressStatsTest {

    private val zone = ZoneId.of("America/Bogota")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun session(start: String, minutes: Int, pages: Int = 0) =
        ReadingSession(bookId = 1, startedAt = at(start), endedAt = at(start) + minutes * 60_000L, pagesRead = pages)

    // Friday, 2 October 2026.
    private val now = at("2026-10-02T21:00:00")

    @Test
    fun todayTheWeekAndItsAverage() {
        val stats = progressStats(
            sessions = listOf(
                session("2026-09-27T10:00:00", minutes = 50), // Sunday of last week: not this week.
                session("2026-09-28T08:00:00", minutes = 30), // Monday
                session("2026-09-30T20:00:00", minutes = 45), // Wednesday
                session("2026-10-02T07:00:00", minutes = 10), // Today, twice.
                session("2026-10-02T19:30:00", minutes = 14),
            ),
            books = emptyList(),
            goalMinutes = 30,
            now = now,
            zone = zone,
        )

        assertEquals(24, stats.todayMinutes)
        assertEquals(0.8f, stats.goalProgress, 0.001f)
        assertEquals(DayOfWeek.MONDAY, stats.week.first().day)
        assertEquals(listOf(30, 0, 45, 0, 24, 0, 0), stats.week.map { it.minutes })
        assertEquals(99, stats.weekMinutes)
        assertEquals(33, stats.weekAverageMinutes)
    }

    @Test
    fun theStreakCountsDaysInARowEvenIfTodayHasNoReadingYet() {
        val sessions = listOf(
            session("2026-09-28T08:00:00", minutes = 5),
            // A paper book's pages count as reading, without minutes.
            session("2026-09-30T08:00:00", minutes = 0, pages = 12),
            session("2026-10-01T08:00:00", minutes = 20),
        )

        val stats = progressStats(sessions, emptyList(), goalMinutes = 30, now = now, zone = zone)

        assertEquals(2, stats.streakDays)
        assertEquals(0, stats.todayMinutes)
    }

    @Test
    fun onlyBooksFinishedThisYearCount() {
        val books = listOf(
            Book(id = 1, title = "A", author = "", status = BookStatus.FINISHED, finishedAt = at("2026-03-01T10:00:00")),
            Book(id = 2, title = "B", author = "", status = BookStatus.FINISHED, finishedAt = at("2025-12-31T10:00:00")),
            // Finished before the app recorded when: unknown year.
            Book(id = 3, title = "C", author = "", status = BookStatus.FINISHED),
            Book(id = 4, title = "D", author = "", status = BookStatus.READING),
        )

        val stats = progressStats(emptyList(), books, goalMinutes = 30, now = now, zone = zone)

        assertEquals(1, stats.finishedThisYear)
        assertEquals(2026, stats.year)
    }
}
