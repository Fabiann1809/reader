package io.github.fabiann1809.reader.data.reader

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.publication.services.positions

/** Why a book could not be opened in the reader. */
enum class OpenProblem {
    /** A paper book, or an imported one whose file is gone. */
    NO_FILE,

    /** A format the reader does not show yet (TXT and CBZ). */
    NOT_SUPPORTED_YET,

    /** The file exists but Readium could not read it (e.g. damaged). */
    UNREADABLE,
}

/** Where the reader is in a book: Readium's Locator as JSON, ready to be saved in [Book.readingLocation]. */
data class ReadingLocation(val bookId: Long, val json: String)

/**
 * Changes made while reading, on top of the saved "Aa" settings: the font size from a pinch (a
 * multiplier, 1 = as published; null = the "Aa" size) and the brightness from the left edge (0 to
 * 1, null = the system's). They last until the book closes.
 */
data class ReadingAdjustments(val fontSize: Double? = null, val brightness: Float? = null)

/**
 * What the reader's controls show: the chapter's title and how far into the book (0 to 1) the page
 * is. [href] is the chapter's file and [position] Readium's position number (about a page; a PDF's
 * page), to highlight the chapter in the index and to know if the page is bookmarked.
 */
data class ReadingPosition(
    val bookId: Long,
    val chapter: String?,
    val progression: Double?,
    val href: String? = null,
    val position: Int? = null,
)

/** Opens a book for reading, keeps it while it is read and reports where the reader is. */
interface ReaderSession {
    /**
     * Null when the book is open and ready, otherwise why it is not. [startAt] (a Locator as JSON,
     * e.g. a note's place) opens it there instead of where the reader left off.
     */
    suspend fun open(book: Book, startAt: String? = null): OpenProblem?

    /** The open publication of [bookId], or null if it is not open (e.g. after the app process was killed). */
    fun publication(bookId: Long): Publication?

    /** Where to start reading [bookId]: the last reported location, or the saved one. Null for the start. */
    fun initialLocator(bookId: Long): Locator?

    /** Called by the navigator on every page turn. */
    fun reportLocation(bookId: Long, locator: Locator)

    /** Page turns, to be saved; one at a time, the newest replacing an unsaved older one. */
    val locations: SharedFlow<ReadingLocation>

    /** The current page of the open book, also when it did not change since it was saved. */
    val position: StateFlow<ReadingPosition?>

    /** Moves the open navigator of [bookId] to [totalProgression] (0 to 1) of the book, e.g. from the progress bar. */
    suspend fun jumpTo(bookId: Long, totalProgression: Double)

    /**
     * How many positions the open book has: the "total" of the page indicator (about a page each;
     * a PDF's pages). Null when unknown.
     */
    fun positionCount(bookId: Long): Int?

    /** The open book's table of contents in reading order; empty when it has none. */
    fun tableOfContents(bookId: Long): List<TocEntry>

    /** Moves the open navigator of [bookId] to the chapter of [entry]. */
    suspend fun jumpToChapter(bookId: Long, entry: TocEntry)

    /** Moves the open navigator of [bookId] to a saved [location] (a Locator as JSON, e.g. a bookmark's). */
    fun jumpToLocation(bookId: Long, location: String)

    /** The page shown now, as a Locator in JSON to bookmark it; null before the navigator reported one. */
    fun currentLocation(bookId: Long): String?

    /** Where the open navigator must go (see [jumpTo]). */
    val jumps: SharedFlow<Locator>

    /** The open book's [ReadingAdjustments]; kept here so they survive the reader being recreated. */
    var adjustments: ReadingAdjustments

    /** The "Aa" settings the navigator shows; the reader's ViewModel keeps them up to date. */
    val readingSettings: StateFlow<ReadingSettings>

    fun applyReadingSettings(settings: ReadingSettings)

    /** The open book's highlights, which the navigator draws; the reader's ViewModel keeps them up to date. */
    val highlights: StateFlow<List<Highlight>>

    fun showHighlights(highlights: List<Highlight>)

    fun close(bookId: Long)
}

/**
 * [ReaderSession] with Readium. Keeps one book in memory at a time: Readium's navigator gets the
 * publication from here when Android recreates the reader (e.g. on rotation), not through a Bundle.
 */
class ReadiumReaderSession(private val readium: ReadiumToolkit, private val bookFiles: BookFiles) : ReaderSession {

    private var openBookId: Long? = null
    private var openPublication: Publication? = null
    private var lastLocator: Locator? = null
    private var toc: List<Pair<TocEntry, Link>> = emptyList()
    private var positionCount: Int? = null

    private val _locations = MutableSharedFlow<ReadingLocation>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val locations: SharedFlow<ReadingLocation> = _locations.asSharedFlow()

    private val _position = MutableStateFlow<ReadingPosition?>(null)
    override val position: StateFlow<ReadingPosition?> = _position.asStateFlow()

    private val _jumps = MutableSharedFlow<Locator>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val jumps: SharedFlow<Locator> = _jumps.asSharedFlow()

    override var adjustments = ReadingAdjustments()

    private val _readingSettings = MutableStateFlow(ReadingSettings())
    override val readingSettings: StateFlow<ReadingSettings> = _readingSettings.asStateFlow()

    override fun applyReadingSettings(settings: ReadingSettings) {
        _readingSettings.value = settings
    }

    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    override val highlights: StateFlow<List<Highlight>> = _highlights.asStateFlow()

    override fun showHighlights(highlights: List<Highlight>) {
        _highlights.value = highlights
    }

    override suspend fun open(book: Book, startAt: String?): OpenProblem? {
        if (book.id == openBookId && openPublication != null) return null
        val path = book.filePath ?: return OpenProblem.NO_FILE
        if (book.format !in READABLE_FORMATS) return OpenProblem.NOT_SUPPORTED_YET
        val file = bookFiles.resolve(path)
        val publication = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            readium.asset(file)?.let { readium.open(it) }
        } ?: return if (file.exists()) OpenProblem.UNREADABLE else OpenProblem.NO_FILE
        closeCurrent()
        openBookId = book.id
        openPublication = publication
        lastLocator = startAt?.let(::parseLocator) ?: book.readingLocation?.let(::parseLocator)
        toc = flattenToc(publication.tableOfContents)
        positionCount = withContext(Dispatchers.IO) { publication.positions().size }.takeIf { it > 0 }
        return null
    }

    override fun publication(bookId: Long): Publication? = openPublication.takeIf { bookId == openBookId }

    override fun initialLocator(bookId: Long): Locator? = lastLocator.takeIf { bookId == openBookId }

    override fun reportLocation(bookId: Long, locator: Locator) {
        if (bookId != openBookId) return
        _position.value = ReadingPosition(
            bookId = bookId,
            chapter = locator.title,
            progression = locator.locations.totalProgression,
            href = locator.href.fileHref(),
            position = locator.locations.position,
        )
        if (locator == lastLocator) return
        lastLocator = locator
        _locations.tryEmit(ReadingLocation(bookId, locator.toJSON().toString()))
    }

    override suspend fun jumpTo(bookId: Long, totalProgression: Double) {
        val publication = publication(bookId) ?: return
        val locator = publication.locateProgression(totalProgression.coerceIn(0.0, 1.0)) ?: return
        _jumps.tryEmit(locator)
    }

    override fun positionCount(bookId: Long): Int? = positionCount.takeIf { bookId == openBookId }

    override fun tableOfContents(bookId: Long): List<TocEntry> = if (bookId == openBookId) toc.map { it.first } else emptyList()

    override suspend fun jumpToChapter(bookId: Long, entry: TocEntry) {
        val publication = publication(bookId) ?: return
        val link = toc.getOrNull(entry.index)?.second ?: return
        publication.locatorFromLink(link)?.let { _jumps.tryEmit(it) }
    }

    override fun jumpToLocation(bookId: Long, location: String) {
        if (bookId != openBookId) return
        parseLocator(location)?.let { _jumps.tryEmit(it) }
    }

    override fun currentLocation(bookId: Long): String? = lastLocator.takeIf { bookId == openBookId }?.toJSON()?.toString()

    override fun close(bookId: Long) {
        if (bookId == openBookId) closeCurrent()
    }

    private fun closeCurrent() {
        _position.value = null
        _highlights.value = emptyList()
        adjustments = ReadingAdjustments()
        openPublication?.close()
        openPublication = null
        openBookId = null
        lastLocator = null
        toc = emptyList()
        positionCount = null
    }

    // A location saved by another Readium version that can't be read just means starting from the beginning.
    private fun parseLocator(json: String): Locator? = try {
        Locator.fromJSON(JSONObject(json))
    } catch (e: JSONException) {
        null
    }

    private companion object {
        val READABLE_FORMATS = setOf(BookFormat.EPUB, BookFormat.PDF)
    }
}
