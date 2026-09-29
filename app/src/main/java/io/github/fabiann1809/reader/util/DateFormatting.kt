package io.github.fabiann1809.reader.util

import java.text.DateFormat
import java.util.Date

/** Formats epoch milliseconds as a medium date in the device locale (e.g. "29 sept 2026"). */
fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
