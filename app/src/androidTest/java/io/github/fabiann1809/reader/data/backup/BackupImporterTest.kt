package io.github.fabiann1809.reader.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.data.session.ReadingSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BackupImporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var oldPhone: AppDatabase
    private lateinit var newPhone: AppDatabase
    private lateinit var root: File

    @Before
    fun setUp() {
        oldPhone = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        newPhone = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        // Scratch folders, so the test never touches the app's real files.
        root = File(context.cacheDir, "import-test").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        oldPhone.close()
        newPhone.close()
        root.deleteRecursively()
    }

    private fun folder(name: String) = File(root, name).apply { mkdirs() }

    private fun importer(filesDir: File) = BackupImporter(newPhone, filesDir, folder("work"))

    /** A backup of a phone with a digital book, its voice note, a card, a session and a collection. */
    private suspend fun backupOfOldPhone(): ByteArray {
        val oldFiles = folder("old")
        File(oldFiles, "books/c.epub").apply { parentFile!!.mkdirs() }.writeText("epub")
        File(oldFiles, "voice/v.m4a").apply { parentFile!!.mkdirs() }.writeText("audio")
        val bookId = oldPhone.bookDao().insert(Book(title = "Cosmos", author = "Carl Sagan", kind = BookKind.DIGITAL, filePath = "books/c.epub"))
        val collectionId = oldPhone.collectionDao().insert(Collection(name = "Ciencia"))
        oldPhone.collectionDao().addBook(BookCollectionCrossRef(bookId, collectionId))
        oldPhone.noteDao().insert(Note(bookId = bookId, content = "Una idea", type = NoteType.VOICE, audioPath = "voice/v.m4a", tag = NoteTag.IDEA))
        oldPhone.flashcardDao().insert(Flashcard(bookId = bookId, front = "¿Qué?", back = "Esto", repetitions = 2, intervalDays = 3))
        oldPhone.readingSessionDao().insert(ReadingSession(bookId = bookId, startedAt = 1, endedAt = 60_001, pagesRead = 4))
        return ByteArrayOutputStream().also { BackupExporter(oldPhone, oldFiles).export(it) }.toByteArray()
    }

    @Test
    fun aBackupRestoresEverythingOnACleanInstall() = runTest {
        val backup = backupOfOldPhone()
        val newFiles = folder("new")

        val summary = importer(newFiles).import(backup.inputStream())

        val book = newPhone.bookDao().getAll().single()
        assertEquals("Cosmos", book.title)
        assertEquals("Ciencia", newPhone.collectionDao().getAll().single().name)
        assertEquals(listOf(book.id), newPhone.collectionDao().getAllLinks().map { it.bookId })
        val note = newPhone.noteDao().getAll().single()
        assertEquals(NoteTag.IDEA, note.tag)
        assertEquals(3, newPhone.flashcardDao().getAll().single().intervalDays)
        assertEquals(4, newPhone.readingSessionDao().getAll().single().pagesRead)
        assertEquals("epub", File(newFiles, "books/c.epub").readText())
        assertEquals("audio", File(newFiles, note.audioPath!!).readText())
        assertEquals(BackupSummary(books = 1, notes = 1, flashcards = 1, files = 2), summary)
    }

    @Test
    fun importingReplacesWhatThePhoneHad() = runTest {
        val backup = backupOfOldPhone()
        val newFiles = folder("new")
        File(newFiles, "books/old.epub").apply { parentFile!!.mkdirs() }.writeText("old")
        newPhone.bookDao().insert(Book(title = "Dune", author = "Frank Herbert", filePath = "books/old.epub"))

        importer(newFiles).import(backup.inputStream())

        assertEquals(listOf("Cosmos"), newPhone.bookDao().observeAll().first().map { it.title })
        assertFalse(File(newFiles, "books/old.epub").exists())
    }

    @Test
    fun aFileThatIsNotABackupChangesNothing() = runTest {
        newPhone.bookDao().insert(Book(title = "Dune", author = "Frank Herbert"))
        val notAZip = "hola".toByteArray()

        try {
            importer(folder("new")).import(notAZip.inputStream())
            fail("Expected an invalid backup")
        } catch (e: BackupError.InvalidFile) {
            // Expected.
        }

        assertEquals(listOf("Dune"), newPhone.bookDao().getAll().map { it.title })
    }

    @Test
    fun aBackupFromANewerAppIsRefused() = runTest {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use {
            it.putNextEntry(ZipEntry("backup.json"))
            it.write("""{"version":99,"exportedAt":1}""".toByteArray())
            it.closeEntry()
        }

        try {
            importer(folder("new")).import(zip.toByteArray().inputStream())
            fail("Expected a newer version")
        } catch (e: BackupError.NewerVersion) {
            assertEquals(99, e.version)
        }
    }

    @Test
    fun aFileTryingToWriteOutsideTheAppIsRefused() = runTest {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use {
            it.putNextEntry(ZipEntry("backup.json"))
            it.write("""{"version":1,"exportedAt":1}""".toByteArray())
            it.closeEntry()
            it.putNextEntry(ZipEntry("files/../../evil.txt"))
            it.write("x".toByteArray())
            it.closeEntry()
        }

        try {
            importer(folder("new")).import(zip.toByteArray().inputStream())
            fail("Expected an invalid backup")
        } catch (e: BackupError.InvalidFile) {
            assertTrue(!File(root, "evil.txt").exists())
        }
    }
}
