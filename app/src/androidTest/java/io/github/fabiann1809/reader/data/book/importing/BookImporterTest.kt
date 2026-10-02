package io.github.fabiann1809.reader.data.book.importing

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
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
import io.github.fabiann1809.reader.data.book.FileHashBackfill
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            fileReader = ReadiumBookFileReader(ReadiumToolkit(context)),
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
    fun theSameFileTwiceIsNotAddedAgain() = runTest {
        val first = importedBook("principito.epub")
        assertNotNull(first.fileHash)

        val again = importer.import(picked("principito.epub").toString())

        assertEquals(ImportResult.AlreadyInLibrary(title = "El principito"), again)
        assertEquals(1, books.observeBooks().first().size)
        // The second copy is not left behind in the app's storage.
        assertEquals(1, storedFiles().size)
    }

    @Test
    fun booksImportedBeforeTheHashGetIt() = runTest {
        val book = importedBook("cosmos.pdf")
        books.updateBook(book.copy(fileHash = null))

        FileHashBackfill(books, bookFiles).run()

        assertEquals(book.fileHash, books.getBook(book.id)?.fileHash)
    }

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
        assertEquals(ImportResult.Unsupported("no_es_un_libro.bin"), importer.import(picked("no_es_un_libro.bin").toString()))
        assertTrue(storedFiles().isEmpty())
    }

    @Test
    fun aFileThatCannotBeOpenedFails() = runTest {
        val missing = Uri.fromFile(File(workDir, "missing.epub"))

        assertEquals(ImportResult.Failed("missing.epub"), importer.import(missing.toString()))
        assertTrue(storedFiles().isEmpty())
    }

    private fun storedCover(book: Book) = BitmapFactory.decodeFile(bookFiles.resolve(book.coverPath!!).path)

    @Test
    fun epubCoverIsStoredScaledDown() = runTest {
        val book = importedBook("principito.epub")

        assertTrue(book.coverPath!!.startsWith("covers/"))
        val cover = storedCover(book)
        // The 600x900 red image of the fixture, scaled to fit 480x720.
        assertEquals(480, cover.width)
        assertEquals(720, cover.height)
        assertTrue(Color.red(cover.getPixel(10, 10)) > 150)
    }

    @Test
    fun pdfCoverIsTheFirstPageOnWhite() = runTest {
        val cover = storedCover(importedBook("cosmos.pdf"))

        // The fixture page is 300x400 points with only a short text near the top; it is enlarged to 480x640.
        assertEquals(480, cover.width)
        assertEquals(640, cover.height)
        val corner = cover.getPixel(cover.width - 2, cover.height - 2)
        assertTrue(Color.red(corner) > 240 && Color.green(corner) > 240 && Color.blue(corner) > 240)
    }

    @Test
    fun deletingTheBookAlsoDeletesItsFileAndCover() = runTest {
        val book = importedBook("principito.epub")

        books.deleteBook(book)

        assertTrue(storedFiles().isEmpty())
        assertTrue(File(workDir, "files/covers").list().isNullOrEmpty())
    }

    @Test
    fun cbzIsImportedWithItsFirstPageAsCover() = runTest {
        val book = importedBook("mafalda_tomo_1.cbz")

        assertEquals(BookFormat.CBZ, book.format)
        // Comics carry no title metadata Readium reads, so the file name is used.
        assertEquals("mafalda tomo 1", book.title)
        assertTrue(book.filePath!!.endsWith(".cbz"))
        // Page 001 is blue; 002 (stored first in the archive) is light grey.
        assertTrue(Color.blue(storedCover(book).getPixel(10, 10)) > 150)
    }

    @Test
    fun txtIsImportedWithTheFileNameAsTitleAndNoCover() = runTest {
        val book = importedBook("cuento_de_navidad.txt")

        assertEquals(BookFormat.TXT, book.format)
        assertEquals("cuento de navidad", book.title)
        assertEquals(null, book.coverPath)
        assertTrue(book.filePath!!.endsWith(".txt"))
    }

    @Test
    fun aTxtFileWithBinaryContentIsRejected() = runTest {
        assertEquals(ImportResult.Unsupported("falso.txt"), importer.import(picked("falso.txt").toString()))
        assertTrue(storedFiles().isEmpty())
    }
}
