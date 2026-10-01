package io.github.fabiann1809.reader.data.reader

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication

/** Why a book could not be opened in the reader. */
enum class OpenProblem {
    /** A paper book, or an imported one whose file is gone. */
    NO_FILE,

    /** A format the reader does not show yet (T11.3 and later). */
    NOT_SUPPORTED_YET,

    /** The file exists but Readium could not read it (e.g. damaged). */
    UNREADABLE,
}

/** Opens a book for reading and keeps it while it is read. */
interface ReaderSession {
    /** Null when the book is open and ready, otherwise why it is not. */
    suspend fun open(book: Book): OpenProblem?

    /** The open publication of [bookId], or null if it is not open (e.g. after the app process was killed). */
    fun publication(bookId: Long): Publication?

    fun close(bookId: Long)
}

/**
 * [ReaderSession] with Readium. Keeps one book in memory at a time: Readium's navigator gets the
 * publication from here when Android recreates the reader (e.g. on rotation), not through a Bundle.
 */
class ReadiumReaderSession(private val readium: ReadiumToolkit, private val bookFiles: BookFiles) : ReaderSession {

    private var openBookId: Long? = null
    private var openPublication: Publication? = null

    override suspend fun open(book: Book): OpenProblem? {
        if (book.id == openBookId && openPublication != null) return null
        val path = book.filePath ?: return OpenProblem.NO_FILE
        if (book.format != BookFormat.EPUB) return OpenProblem.NOT_SUPPORTED_YET
        val file = bookFiles.resolve(path)
        val publication = withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            readium.asset(file)?.let { readium.open(it) }
        } ?: return if (file.exists()) OpenProblem.UNREADABLE else OpenProblem.NO_FILE
        closeCurrent()
        openBookId = book.id
        openPublication = publication
        return null
    }

    override fun publication(bookId: Long): Publication? = openPublication.takeIf { bookId == openBookId }

    override fun close(bookId: Long) {
        if (bookId == openBookId) closeCurrent()
    }

    private fun closeCurrent() {
        openPublication?.close()
        openPublication = null
        openBookId = null
    }
}
