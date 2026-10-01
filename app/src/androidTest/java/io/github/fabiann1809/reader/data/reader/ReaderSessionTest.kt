package io.github.fabiann1809.reader.data.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import java.io.File

/** Real Readium on the small books in androidTest/assets/books. */
@RunWith(AndroidJUnit4::class)
class ReaderSessionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val filesDir = File(context.cacheDir, "reader-session-test").apply { mkdirs() }
    private val bookFiles = BookFiles(filesDir)
    private val session = ReadiumReaderSession(ReadiumToolkit(context), bookFiles)

    @After
    fun tearDown() {
        session.close(1)
        filesDir.deleteRecursively()
    }

    /** A digital book whose stored file is a copy of [asset]. */
    private fun bookWith(asset: String, format: BookFormat): Book {
        val file = bookFiles.newFile(format.name.lowercase())
        InstrumentationRegistry.getInstrumentation().context.assets.open("books/$asset").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return Book(id = 1, title = asset, author = "", kind = BookKind.DIGITAL, format = format, filePath = bookFiles.relativePath(file))
    }

    @Test
    fun anEpubOpensAndStaysAvailableUntilClosed() = runTest {
        assertNull(session.open(bookWith("principito.epub", BookFormat.EPUB)))

        assertEquals("El principito", session.publication(1)?.metadata?.title)
        assertNull(session.publication(2))

        session.close(1)
        assertNull(session.publication(1))
    }

    @Test
    fun otherFormatsAreNotReadYet() = runTest {
        assertEquals(OpenProblem.NOT_SUPPORTED_YET, session.open(bookWith("cosmos.pdf", BookFormat.PDF)))
    }

    @Test
    fun aMissingOrDamagedFileSaysWhy() = runTest {
        val paperBook = Book(id = 1, title = "Papel", author = "")
        assertEquals(OpenProblem.NO_FILE, session.open(paperBook))

        val damaged = bookWith("no_es_un_libro.bin", BookFormat.EPUB)
        assertEquals(OpenProblem.UNREADABLE, session.open(damaged))

        bookFiles.resolve(damaged.filePath!!).delete()
        assertEquals(OpenProblem.NO_FILE, session.open(damaged))
    }

    @Test
    fun theReaderStartsWhereItWasLeftAndReportsPageTurns() = runTest {
        val saved = """{"href":"OEBPS/chapter1.xhtml","type":"application/xhtml+xml","locations":{"progression":0.5}}"""
        val book = bookWith("principito.epub", BookFormat.EPUB).copy(readingLocation = saved)
        assertNull(session.open(book))

        val start = session.initialLocator(1)!!
        assertEquals(0.5, start.locations.progression!!, 0.0001)

        val reported = async(start = CoroutineStart.UNDISPATCHED) { session.locations.first() }
        session.reportLocation(1, start.copyWithLocations(progression = 0.75))
        assertEquals(0.75, Locator.fromJSON(JSONObject(reported.await().json))!!.locations.progression!!, 0.0001)
        // A rotation recreates the navigator from the last page turn.
        assertEquals(0.75, session.initialLocator(1)!!.locations.progression!!, 0.0001)
    }

    @Test
    fun anUnreadableSavedLocationStartsFromTheBeginning() = runTest {
        val book = bookWith("principito.epub", BookFormat.EPUB).copy(readingLocation = "not json")

        assertNull(session.open(book))
        assertNull(session.initialLocator(1))
    }
}
