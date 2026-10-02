package io.github.fabiann1809.reader.util

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The last millisecond of the day [millis] falls in, in [zone]: a card due any time today counts as
 * "for today" (T14.3), even if its hour hasn't come yet.
 */
fun endOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

/** How many calendar days after [now]'s day [millis] falls, in [zone]: 0 today, 1 tomorrow… */
fun daysFromToday(millis: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(today, day)
}
