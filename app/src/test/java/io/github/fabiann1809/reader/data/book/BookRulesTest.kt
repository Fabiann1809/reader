package io.github.fabiann1809.reader.data.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BookRulesTest {

    private val now = TimeUnit.DAYS.toMillis(100)
    private val addedToday = Book(title = "Dune", author = "Frank Herbert", createdAt = now - 1_000)

    @Test
    fun aBookAddedThisWeekAndNotStartedIsNew() {
        assertTrue(addedToday.isNew(now))
    }

    @Test
    fun aBookStopsBeingNewAfterAWeekOrOnceStarted() {
        assertFalse(addedToday.copy(createdAt = now - TimeUnit.DAYS.toMillis(8)).isNew(now))
        assertFalse(addedToday.copy(currentPage = 3).isNew(now))
        assertFalse(addedToday.copy(status = BookStatus.READING).isNew(now))
        assertFalse(addedToday.copy(lastOpenedAt = now).isNew(now))
    }

    @Test
    fun markedAsReadFinishesAndMovesToTheLastKnownPage() {
        val withTotal = addedToday.copy(currentPage = 40, totalPages = 300).markedAsRead()
        assertEquals(BookStatus.FINISHED, withTotal.status)
        assertEquals(300, withTotal.currentPage)

        val withoutTotal = addedToday.copy(currentPage = 40).markedAsRead()
        assertEquals(BookStatus.FINISHED, withoutTotal.status)
        assertEquals(40, withoutTotal.currentPage)
    }
}
