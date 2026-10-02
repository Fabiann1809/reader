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
}
