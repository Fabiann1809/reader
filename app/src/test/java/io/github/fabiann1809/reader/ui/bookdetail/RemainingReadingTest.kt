package io.github.fabiann1809.reader.ui.bookdetail

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.session.ReadingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemainingReadingTest {

    private val book = Book(id = 1, title = "El arte de aprender", author = "", currentPage = 134, totalPages = 320)

    private fun session(minutes: Int, pages: Int) =
        ReadingSession(bookId = 1, startedAt = 0, endedAt = minutes * 60_000L, pagesRead = pages)

    @Test
    fun thePagesLeftAndTheTimeAtTheReadersPace() {
        // 30 minutes for 28 pages, and a paper session with no time that doesn't count for the pace.
        val remaining = remainingReading(book, listOf(session(20, 18), session(10, 10), session(0, 40)))!!

        assertEquals(186, remaining.pages)
        assertEquals(199, remaining.minutes)
    }

    @Test
    fun withoutAPaceOnlyThePagesAreKnown() {
        assertEquals(RemainingReading(186, minutes = null), remainingReading(book, emptyList()))
    }

    @Test
    fun withoutAPageCountNothingIsKnown() {
        assertNull(remainingReading(book.copy(totalPages = null), listOf(session(10, 5))))
    }
}
