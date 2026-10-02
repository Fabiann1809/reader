package io.github.fabiann1809.reader.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DaysTest {

    private val bogota = ZoneId.of("America/Bogota")

    private fun millis(text: String) = LocalDateTime.parse(text).atZone(bogota).toInstant().toEpochMilli()

    @Test
    fun theDayEndsJustBeforeTheNextMidnightInTheUsersZone() {
        val end = endOfDay(millis("2026-10-02T08:30:00"), bogota)

        assertEquals(millis("2026-10-03T00:00:00") - 1, end)
        assertEquals(end, endOfDay(millis("2026-10-02T23:59:59"), bogota))
    }

    @Test
    fun daysFromTodayCountsCalendarDaysNotHours() {
        val now = millis("2026-10-02T23:00:00")

        assertEquals(0, daysFromToday(millis("2026-10-02T23:30:00"), now, bogota))
        // One hour later, but already tomorrow.
        assertEquals(1, daysFromToday(millis("2026-10-03T00:30:00"), now, bogota))
        assertEquals(4, daysFromToday(millis("2026-10-06T08:00:00"), now, bogota))
    }
}
