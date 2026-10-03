package io.github.fabiann1809.reader.ui.bookdetail

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.session.ReadingSession
import kotlin.math.roundToInt

/** What is left of a book (T16.4): "186 páginas restantes · 3 h 20 m estimadas". */
data class RemainingReading(val pages: Int, val minutes: Int?)

/**
 * The pages left of [book] and how long they would take at the reader's pace in its [sessions].
 * Null when the page count is unknown; no estimate until there is a pace to go by.
 */
fun remainingReading(book: Book, sessions: List<ReadingSession>): RemainingReading? {
    val total = book.totalPages?.takeIf { it > 0 } ?: return null
    val pages = (total - book.currentPage).coerceAtLeast(0)
    val minutes = if (book.kind == BookKind.DIGITAL) digitalEstimate(book, total, sessions) else pageEstimate(pages, sessions)
    return RemainingReading(pages, minutes)
}

// A digital book is read only in the app, from its start: the time so far, for the share read so far.
// (Its "pages" are Readium positions, which don't match the screens counted in the sessions.)
private fun digitalEstimate(book: Book, total: Int, sessions: List<ReadingSession>): Int? {
    val read = book.currentPage.toDouble() / total
    val minutes = sessions.sumOf { it.durationMillis } / MILLIS_PER_MINUTE
    if (read <= 0.0 || minutes <= 0.0) return null
    return (minutes * (1 - read) / read).roundToInt()
}

// Minutes per page of the timed sessions; paper books' sessions have pages but no time.
private fun pageEstimate(pages: Int, sessions: List<ReadingSession>): Int? {
    val timed = sessions.filter { it.durationMillis > 0 && it.pagesRead > 0 }
    if (timed.isEmpty()) return null
    val minutesPerPage = timed.sumOf { it.durationMillis } / MILLIS_PER_MINUTE / timed.sumOf { it.pagesRead }
    return (pages * minutesPerPage).roundToInt()
}

private const val MILLIS_PER_MINUTE = 60_000.0
