package io.github.fabiann1809.reader.data.book.importing

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.DefaultBookRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real Readium and PDFium on the small files in androidTest/assets/books. */
@RunWith(AndroidJUnit4::class)
class BookImporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase
    private lateinit var workDir: File
    private lateinit var bookFiles: BookFiles
    private lateinit var books: BookRepository
    private lateinit var importer: DefaultBookImporter

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        workDir = File(context.cacheDir, "import-test").apply { mkdirs() }
        bookFiles = BookFiles(File(workDir, "files"))
        books = DefaultBookRepository(database.bookDao(), bookFiles)
        importer = DefaultBookImporter(
            contentResolver = context.contentResolver,
            bookFiles = bookFiles,
            fileReader = ReadiumBookFileReader(context),
            bookRepository = books,
            untitled = "Libro sin título",
        )
    }

    @After
    fun tearDown() {
        database.close()
        workDir.deleteRecursively()
    }

    /** Copies a test asset out of the test APK, as if the picker had handed it over. */
    private fun picked(asset: String): Uri {
        val file = File(workDir, asset)
        InstrumentationRegistry.getInstrumentation().context.assets.open("books/$asset").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(file)
    }

    private suspend fun importedBook(asset: String): Book {
        val result = importer.import(picked(asset).toString())
        assertTrue("Expected Imported, got $result", result is ImportResult.Imported)
        return books.getBook((result as ImportResult.Imported).bookId)!!
    }

    private fun storedFiles(): List<String> = File(workDir, "files/books").list()?.toList().orEmpty()

    @Test
    fun epubIsImportedWithItsMetadata() = runTest {
        val book = importedBook("principito.epub")

        assertEquals("El principito", book.title)
        assertEquals("Antoine de Saint-Exupéry", book.author)
        assertEquals("es", book.language)
        assertEquals(BookKind.DIGITAL, book.kind)
        assertEquals(BookFormat.EPUB, book.format)
        assertTrue(book.filePath!!.startsWith("books/") && book.filePath!!.endsWith(".epub"))
        assertTrue(bookFiles.resolve(book.filePath!!).exists())
    }

    @Test
    fun pdfIsImportedWithItsMetadata() = runTest {
        val book = importedBook("cosmos.pdf")

        assertEquals("Cosmos", book.title)
        assertEquals("Carl Sagan", book.author)
        assertEquals(BookFormat.PDF, book.format)
        assertTrue(book.filePath!!.endsWith(".pdf"))
    }

    @Test
    fun pdfWithoutMetadataTakesItsTitleFromTheFileName() = runTest {
        val book = importedBook("sin_metadatos.pdf")

        assertEquals("sin metadatos", book.title)
        assertEquals("", book.author)
    }

    @Test
    fun aFileThatIsNotABookIsRejectedAndNotKept() = runTest {
        assertEquals(ImportResult.Unsupported, importer.import(picked("no_es_un_libro.bin").toString()))
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun aFileThatCannotBeOpenedFails() = runTest {
        val missing = Uri.fromFile(File(workDir, "missing.epub"))

        assertEquals(ImportResult.Failed, importer.import(missing.toString()))
        assertTrue(storedFiles().isEmpty())
    }
}
