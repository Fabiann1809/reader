package io.github.fabiann1809.reader.ui.bookdetail

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.session.ReadingSession
import kotlin.math.roundToInt

/** What is left of a book (T16.4): "186 páginas restantes · 3 h 20 m estimadas". */
data class RemainingReading(val pages: Int, val minutes: Int?)

/**
 * The pages left of [book] and, from the reader's pace in its [sessions] (minutes per page of the
 * timed ones), how long they would take. Null when the page count is unknown; no estimate until
 * there is a pace to go by.
 */
fun remainingReading(book: Book, sessions: List<ReadingSession>): RemainingReading? {
    val total = book.totalPages ?: return null
    val pages = (total - book.currentPage).coerceAtLeast(0)
    // Paper books' sessions have pages but no time: they say nothing about the pace.
    val timed = sessions.filter { it.durationMillis > 0 && it.pagesRead > 0 }
    val minutesPerPage = if (timed.isEmpty()) null else timed.sumOf { it.durationMillis } / MILLIS_PER_MINUTE / timed.sumOf { it.pagesRead }
    return RemainingReading(pages, minutesPerPage?.let { (pages * it).roundToInt() })
}

private const val MILLIS_PER_MINUTE = 60_000.0
