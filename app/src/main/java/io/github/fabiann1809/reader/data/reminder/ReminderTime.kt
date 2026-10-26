package io.github.fabiann1809.reader.data.reminder

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** 8 p. m.: the hour the reminder starts at until the user picks another one. */
const val DEFAULT_REMINDER_MINUTES = 20 * 60

const val MINUTES_PER_HOUR = 60

/** Milliseconds from [now] to the next [minutesOfDay] (minutes after midnight): today if it is still ahead, else tomorrow. */
fun millisUntilNext(minutesOfDay: Int, now: ZonedDateTime): Long {
    val today = clockTimeOn(now.toLocalDate(), minutesOfDay, now)
    val next = if (today.isAfter(now)) today else clockTimeOn(now.toLocalDate().plusDays(1), minutesOfDay, now)
    return Duration.between(now, next).toMillis()
}

// Built from the clock time, not by adding hours, so a daylight saving change keeps the same clock time.
private fun clockTimeOn(date: LocalDate, minutesOfDay: Int, now: ZonedDateTime): ZonedDateTime =
    date.atTime(minutesOfDay / MINUTES_PER_HOUR, minutesOfDay % MINUTES_PER_HOUR).atZone(now.zone)
