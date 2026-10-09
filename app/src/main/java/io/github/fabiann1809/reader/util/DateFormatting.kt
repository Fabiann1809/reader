package io.github.fabiann1809.reader.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** The app's text is Spanish, so dates and day names are too, whatever the device language is. */
val AppLocale: Locale = Locale.forLanguageTag("es")

/** Formats epoch milliseconds as a medium date in Spanish (e.g. "29 sept 2026"). */
fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, AppLocale).format(Date(epochMillis))
