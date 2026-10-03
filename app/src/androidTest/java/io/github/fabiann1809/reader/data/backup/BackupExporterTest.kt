package io.github.fabiann1809.reader.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.data.session.ReadingSession
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class BackupExporterTest {

    private lateinit var database: AppDatabase
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        // A scratch folder, so the test never touches the app's real files.
        filesDir = File(context.cacheDir, "backup-test").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        database.close()
        filesDir.deleteRecursively()
    }

    private fun file(path: String, text: String) = File(filesDir, path).apply {
        parentFile!!.mkdirs()
        writeText(text)
    }

    /** The ZIP's entries, by name, with their bytes as text. */
    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry -> entries[entry.name] = zip.readBytes().decodeToString() }
        }
        return entries
    }

    @Test
    fun theZipHoldsEveryTableAndTheFilesTheyPointTo() = runTest {
        file("books/a.epub", "epub")
        file("covers/a.jpg", "cover")
        file("voice/v.m4a", "audio")
        val bookId = database.bookDao().insert(
            Book(title = "Cosmos", author = "Carl Sagan", kind = BookKind.DIGITAL, filePath = "books/a.epub", coverPath = "covers/a.jpg"),
        )
        // A paper book with a cover that was deleted outside the app.
        database.bookDao().insert(Book(title = "Dune", author = "Frank Herbert", coverPath = "covers/gone.jpg"))
        database.noteDao().insert(Note(bookId = bookId, content = "Una idea", type = NoteType.VOICE, audioPath = "voice/v.m4a"))
        database.flashcardDao().insert(Flashcard(bookId = bookId, front = "¿Qué?", back = "Esto"))
        database.readingSessionDao().insert(ReadingSession(bookId = bookId, startedAt = 1, endedAt = 60_001, pagesRead = 3))

        val output = ByteArrayOutputStream()
        val summary = BackupExporter(database, filesDir, now = { 42L }).export(output)

        val entries = unzip(output.toByteArray())
        assertEquals(setOf("backup.json", "files/books/a.epub", "files/covers/a.jpg", "files/voice/v.m4a"), entries.keys)
        assertEquals("audio", entries["files/voice/v.m4a"])
        val data = Json { ignoreUnknownKeys = true }.decodeFromString(BackupData.serializer(), entries.getValue("backup.json"))
        assertEquals(BackupData.CURRENT_VERSION, data.version)
        assertEquals(42L, data.exportedAt)
        assertEquals(setOf("Cosmos", "Dune"), data.books.map { it.title }.toSet())
        assertEquals("voice/v.m4a", data.notes.single().audioPath)
        assertEquals(3, data.sessions.single().pagesRead)
        assertEquals(BackupSummary(books = 2, notes = 1, flashcards = 1, files = 3), summary)
    }

    @Test
    fun theApiKeyIsNeverInTheBackup() = runTest {
        database.bookDao().insert(Book(title = "Cosmos", author = "Carl Sagan"))
        val output = ByteArrayOutputStream()

        BackupExporter(database, filesDir).export(output)

        val text = unzip(output.toByteArray()).values.joinToString()
        assertFalse(text.contains("api", ignoreCase = true) && text.contains("key", ignoreCase = true))
        assertTrue(text.contains("Cosmos"))
    }
}
