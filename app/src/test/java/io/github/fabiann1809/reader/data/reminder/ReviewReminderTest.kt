package io.github.fabiann1809.reader.data.reminder

import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.prefs.AppSettings
import io.github.fabiann1809.reader.testing.FakeAppSettingsStore
import io.github.fabiann1809.reader.testing.FakeFlashcardRepository
import io.github.fabiann1809.reader.testing.FakeReviewReminderScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReviewReminderTest {

    private val now = 1_000_000L
    private val settings = FakeAppSettingsStore(AppSettings(reviewReminder = true, reminderMinutes = 21 * 60 + 30))
    private val cards = FakeFlashcardRepository()
    private val scheduler = FakeReviewReminderScheduler()
    private val notified = mutableListOf<Int>()
    private val reminder = ReviewReminder(settings, cards, { notified += it }, scheduler, now = { now })

    private suspend fun addCard(nextReviewAt: Long) =
        cards.addFlashcard(Flashcard(bookId = 1, front = "¿?", back = "!", nextReviewAt = nextReviewAt))

    @Test
    fun saysHowManyCardsAreDueAndSetsTomorrowsReminder() = runTest {
        addCard(now - 10)
        addCard(now)
        addCard(now + 5_000)

        reminder.deliver()

        assertEquals(listOf(2), notified)
        assertEquals(listOf(21 * 60 + 30), scheduler.scheduled)
    }

    @Test
    fun staysQuietWithoutDueCardsButKeepsTheReminder() = runTest {
        addCard(now + 5_000)

        reminder.deliver()

        assertTrue(notified.isEmpty())
        assertEquals(listOf(21 * 60 + 30), scheduler.scheduled)
    }

    @Test
    fun doesNothingOnceTurnedOff() = runTest {
        settings.update { it.copy(reviewReminder = false) }
        addCard(now - 10)

        reminder.deliver()

        assertTrue(notified.isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun theNextReminderIsTodayIfTheHourIsAheadElseTomorrow() {
        val zone = ZoneId.of("America/Bogota")
        val nineAm = ZonedDateTime.of(2026, 10, 26, 9, 0, 0, 0, zone)

        assertEquals(11 * 60 * 60_000L, millisUntilNext(20 * 60, nineAm))
        assertEquals(24 * 60 * 60_000L, millisUntilNext(9 * 60, nineAm))
        assertEquals(23 * 60 * 60_000L, millisUntilNext(8 * 60, nineAm))
    }

    @Test
    fun theHourKeepsItsClockTimeAcrossADaylightSavingChange() {
        val zone = ZoneId.of("America/New_York")
        // The clocks go forward on 2026-03-08: that night lasts 23 hours.
        val evening = ZonedDateTime.of(2026, 3, 7, 22, 0, 0, 0, zone)

        // 22:00 to 07:00 is 9 hours on the clock, 8 real hours that night.
        assertEquals(8 * 60 * 60_000L, millisUntilNext(7 * 60, evening))
    }
}
