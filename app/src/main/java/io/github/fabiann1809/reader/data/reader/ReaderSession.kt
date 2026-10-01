package io.github.fabiann1809.reader.data.reader

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

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

/** Opens a book for reading, keeps it while it is read and reports where the reader is. */
interface ReaderSession {
    /** Null when the book is open and ready, otherwise why it is not. */
    suspend fun open(book: Book): OpenProblem?

    /** The open publication of [bookId], or null if it is not open (e.g. after the app process was killed). */
    fun publication(bookId: Long): Publication?

    /** Where to start reading [bookId]: the last reported location, or the saved one. Null for the start. */
    fun initialLocator(bookId: Long): Locator?

    /** Called by the navigator on every page turn. */
    fun reportLocation(bookId: Long, locator: Locator)

    /** Page turns, to be saved; one at a time, the newest replacing an unsaved older one. */
    val locations: SharedFlow<ReadingLocation>

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

    private val _locations = MutableSharedFlow<ReadingLocation>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val locations: SharedFlow<ReadingLocation> = _locations.asSharedFlow()

    override suspend fun open(book: Book): OpenProblem? {
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
        lastLocator = book.readingLocation?.let(::parseLocator)
        return null
    }

    override fun publication(bookId: Long): Publication? = openPublication.takeIf { bookId == openBookId }

    override fun initialLocator(bookId: Long): Locator? = lastLocator.takeIf { bookId == openBookId }

    override fun reportLocation(bookId: Long, locator: Locator) {
        if (bookId != openBookId || locator == lastLocator) return
        lastLocator = locator
        _locations.tryEmit(ReadingLocation(bookId, locator.toJSON().toString()))
    }

    override fun close(bookId: Long) {
        if (bookId == openBookId) closeCurrent()
    }

    private fun closeCurrent() {
        openPublication?.close()
        openPublication = null
        openBookId = null
        lastLocator = null
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
