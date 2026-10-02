package io.github.fabiann1809.reader.util

import java.time.Instant
import java.time.ZoneId

/**
 * The last millisecond of the day [millis] falls in, in [zone]: a card due any time today counts as
 * "for today" (T14.3), even if its hour hasn't come yet.
 */
fun endOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
