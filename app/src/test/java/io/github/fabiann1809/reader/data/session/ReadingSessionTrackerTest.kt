package io.github.fabiann1809.reader.data.session

import io.github.fabiann1809.reader.testing.FakeReadingSessionRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReadingSessionTrackerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeReadingSessionRepository()
    private var clock = 1_000_000L
    private val tracker = ReadingSessionTracker(repository, mainDispatcherRule.testScope(), now = { clock })

    @Test
    fun aSessionKeepsItsTimeAndEachPageMovedForward() {
        tracker.start(bookId = 4)
        // The first report is where reading starts, not a page read. Two screens can share the
        // book's progression: the chapter's tells them apart.
        tracker.moved(0.10, 0.5)
        tracker.moved(0.10, 0.6)
        tracker.moved(0.11, 0.7)
        tracker.moved(0.12, 0.0)
        clock += 20 * 60_000L

        tracker.stop()

        val session = repository.currentSessions.single()
        assertEquals(4, session.bookId)
        assertEquals(1_000_000L, session.startedAt)
        assertEquals(20 * 60_000L, session.durationMillis)
        assertEquals(3, session.pagesRead)
        assertFalse(tracker.isRunning)
    }

    @Test
    fun readingAgainAfterGoingBackDoesNotCount() {
        tracker.start(bookId = 4)
        listOf(0.10, 0.11, 0.12, 0.11, 0.12, 0.13).forEach { tracker.moved(it, 0.0) }
        clock += 60_000L

        tracker.stop()
        tracker.stop()

        assertEquals(3, repository.currentSessions.single().pagesRead)
    }

    @Test
    fun aGlanceIsNotASession() {
        tracker.start(bookId = 4)
        tracker.moved(0.3, 0.1)
        clock += 10_000L

        tracker.stop()

        assertTrue(repository.currentSessions.isEmpty())
    }
}
