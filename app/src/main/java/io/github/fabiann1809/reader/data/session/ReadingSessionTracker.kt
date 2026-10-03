package io.github.fabiann1809.reader.data.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Times a reading session in the reader (T16.1): [start] when the book shows, [moved] on every page
 * the navigator reports, [stop] when the reader leaves or the app goes to the background. Saving
 * happens in [scope], which outlives the reader, so leaving the screen doesn't lose the session.
 */
class ReadingSessionTracker(
    private val repository: ReadingSessionRepository,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var bookId: Long? = null
    private var startedAt = 0L
    private var furthest: ReadingPoint? = null
    private var pagesRead = 0

    val isRunning: Boolean get() = bookId != null

    fun start(bookId: Long) {
        if (isRunning) return
        this.bookId = bookId
        startedAt = now()
        furthest = null
        pagesRead = 0
    }

    /**
     * A page shown at [progression] through the book and [chapterProgression] through its chapter
     * (both 0 to 1). Each page further than any before in this session counts as one read; going
     * back and reading again doesn't. The book's progression alone is too coarse (several screens
     * share one Readium position), so the chapter's breaks the tie.
     */
    fun moved(progression: Double?, chapterProgression: Double?) {
        if (!isRunning || progression == null) return
        val point = ReadingPoint(progression, chapterProgression ?: 0.0)
        val before = furthest
        if (before != null && point > before) pagesRead++
        if (before == null || point > before) furthest = point
    }

    private data class ReadingPoint(val book: Double, val chapter: Double) : Comparable<ReadingPoint> {
        override fun compareTo(other: ReadingPoint): Int = compareValuesBy(this, other, { it.book }, { it.chapter })
    }

    /** Ends the session and saves it, unless it was too short to be real reading. */
    fun stop() {
        val book = bookId ?: return
        bookId = null
        val endedAt = now()
        if (endedAt - startedAt < MIN_SESSION_MILLIS) return
        val session = ReadingSession(bookId = book, startedAt = startedAt, endedAt = endedAt, pagesRead = pagesRead)
        scope.launch { repository.addSession(session) }
    }

    private companion object {
        // Opening a book to glance at it, or passing through, isn't a reading session.
        const val MIN_SESSION_MILLIS = 30_000L
    }
}
