package io.github.fabiann1809.reader.data.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookStatusRulesTest {

    private val book = Book(id = 1, title = "Cosmos", author = "Carl Sagan", currentPage = 10, totalPages = 300)

    @Test
    fun finishingABookRecordsWhenAndKeepsItIfFinishedAgain() {
        val finished = book.withStatus(BookStatus.FINISHED, now = 5_000)
        assertEquals(5_000L, finished.finishedAt)

        assertEquals(5_000L, finished.withStatus(BookStatus.FINISHED, now = 9_000).finishedAt)
    }

    @Test
    fun readingItAgainForgetsWhenItWasFinished() {
        val reading = book.withStatus(BookStatus.FINISHED, now = 5_000).withStatus(BookStatus.READING, now = 6_000)

        assertNull(reading.finishedAt)
    }

    @Test
    fun markingAsReadMovesToTheLastPage() {
        val read = book.markedAsRead(now = 7_000)

        assertEquals(BookStatus.FINISHED, read.status)
        assertEquals(300, read.currentPage)
        assertEquals(7_000L, read.finishedAt)
    }
}
