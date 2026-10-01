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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
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
}
