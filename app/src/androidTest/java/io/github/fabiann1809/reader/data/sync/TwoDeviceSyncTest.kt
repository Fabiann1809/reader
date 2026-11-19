package io.github.fabiann1809.reader.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.sha256
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.data.voice.VoiceFiles
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** Two devices, each with its own Room database and files, syncing through one fake cloud (T18.5). */
@RunWith(AndroidJUnit4::class)
class TwoDeviceSyncTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val cloud = FakeCloudStorage()
    private val devices = mutableListOf<Device>()

    private inner class Device(name: String) {
        val dir = tmp.newFolder(name)
        val database: AppDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).addCallback(SyncTriggersCallback).build()
        val bookFiles = BookFiles(dir)
        val voiceFiles = VoiceFiles(dir)
        private val local = RoomLocalSyncData(
            database,
            LocalSnapshotReader(database, bookFiles, voiceFiles),
            LocalChangeApplier(database, bookFiles, voiceFiles),
        )
        private val repository = DefaultSyncRepository(local, cloud)

        init {
            devices += this
        }

        suspend fun sync() = repository.sync().getOrThrow()

        /** A digital book with its file and cover; returns the stored row. */
        suspend fun addBook(title: String, content: String, cover: String, fileHash: String = "hash-$title"): Book {
            val file = bookFiles.newFile("epub").apply { writeText(content) }
            val coverFile = bookFiles.newCoverFile().apply { writeText(cover) }
            val id = database.bookDao().insert(
                Book(
                    title = title, author = "Author", kind = BookKind.DIGITAL, filePath = bookFiles.relativePath(file),
                    coverPath = bookFiles.relativePath(coverFile), fileHash = fileHash,
                ),
            )
            return database.bookDao().getById(id)!!
        }

        suspend fun addVoiceNote(bookId: Long, audio: String): Note {
            val recording = voiceFiles.newSyncedRecording().apply { writeText(audio) }
            val id = database.noteDao().insert(
                Note(bookId = bookId, content = "voice", audioPath = voiceFiles.storedPath(recording)),
            )
            return database.noteDao().getById(id)!!
        }

        suspend fun books() = database.bookDao().getAll()
        suspend fun notes() = database.noteDao().getAll()
        fun file(path: String) = bookFiles.resolve(path)
    }

    @After
    fun tearDown() {
        devices.forEach { it.database.close() }
    }

    @Test
    fun aWholeLibraryTravelsToTheOtherDeviceWithItsFilesAndRelations() = runTest {
        val a = Device("a")
        val b = Device("b")
        val book = a.addBook("Dune", "the epub", "the cover")
        val note = a.addVoiceNote(book.id, "the audio")
        val collectionId = a.database.collectionDao().insert(Collection(name = "Clásicos"))
        a.database.collectionDao().addBook(BookCollectionCrossRef(book.id, collectionId))
        a.database.flashcardDao().insert(Flashcard(bookId = book.id, front = "Q", back = "A"))
        a.database.bookmarkDao().insert(Bookmark(bookId = book.id, location = "{}"))
        a.database.highlightDao().insert(Highlight(bookId = book.id, location = "{}", text = "t"))
        a.database.readingSessionDao().insert(ReadingSession(bookId = book.id, startedAt = 1, endedAt = 2, pagesRead = 3))

        a.sync()
        b.sync()

        val copy = b.books().single()
        assertEquals(book.syncId, copy.syncId)
        assertEquals("Dune", copy.title)
        assertEquals("the epub", b.file(copy.filePath!!).readText())
        assertEquals("the cover", b.file(copy.coverPath!!).readText())
        // The local paths are this device's own, not A's.
        assertNotEquals(book.filePath, copy.filePath)
        val copiedNote = b.notes().single()
        assertEquals(copy.id, copiedNote.bookId)
        assertEquals(note.syncId, copiedNote.syncId)
        assertEquals("the audio", b.file(copiedNote.audioPath!!).readText())
        assertEquals(1, b.database.collectionDao().getAll().size)
        assertEquals(1, b.database.collectionDao().getAllLinks().size)
        assertEquals(1, b.database.flashcardDao().getAll().size)
        assertEquals(1, b.database.bookmarkDao().getAll().size)
        assertEquals(1, b.database.highlightDao().getAll().size)
        assertEquals(3, b.database.readingSessionDao().getAll().single().pagesRead)
    }

    @Test
    fun editsAndDeletionsTravelBack() = runTest {
        val a = Device("a")
        val b = Device("b")
        val book = a.addBook("Dune", "epub", "cover")
        val noteId = a.database.noteDao().insert(Note(bookId = book.id, content = "first"))
        a.database.flashcardDao().insert(Flashcard(bookId = book.id, front = "Q", back = "A"))
        val collectionId = a.database.collectionDao().insert(Collection(name = "Clásicos"))
        a.database.collectionDao().addBook(BookCollectionCrossRef(book.id, collectionId))
        a.sync()
        b.sync()

        val onB = b.notes().single()
        b.database.noteDao().update(onB.copy(content = "edited on B"))
        b.database.flashcardDao().delete(b.database.flashcardDao().getAll().single())
        b.database.collectionDao().removeBook(b.books().single().id, b.database.collectionDao().getAll().single().id)
        b.sync()
        a.sync()

        assertEquals("edited on B", a.database.noteDao().getById(noteId)!!.content)
        assertTrue(a.database.flashcardDao().getAll().isEmpty())
        assertTrue(a.database.collectionDao().getAllLinks().isEmpty())
        // The collection itself stays.
        assertEquals(1, a.database.collectionDao().getAll().size)
    }

    @Test
    fun deletingABookRemovesItsFilesOnTheOtherDeviceAndInTheCloud() = runTest {
        val a = Device("a")
        val b = Device("b")
        val book = a.addBook("Dune", "epub", "cover")
        a.addVoiceNote(book.id, "audio")
        a.sync()
        b.sync()
        val onB = b.books().single()
        val bookFile = b.file(onB.filePath!!)
        val coverFile = b.file(onB.coverPath!!)
        val audioFile = b.file(b.notes().single().audioPath!!)
        assertTrue(bookFile.exists())

        a.database.bookDao().delete(book)
        a.sync()
        b.sync()

        assertTrue(b.books().isEmpty())
        assertTrue(b.notes().isEmpty())
        assertFalse(bookFile.exists())
        assertFalse(coverFile.exists())
        assertFalse(audioFile.exists())
        assertTrue(cloud.files.isEmpty())
    }

    @Test
    fun theSameBookImportedOnBothDevicesEndsUpAsOneBook() = runTest {
        val a = Device("a")
        val b = Device("b")
        val onA = a.addBook("Dune", "epub", "cover", fileHash = "same")
        b.addBook("Dune", "epub", "cover", fileHash = "same")

        a.sync()
        b.sync()
        a.sync()

        assertEquals(listOf(onA.syncId), a.books().map { it.syncId })
        assertEquals(listOf(onA.syncId), b.books().map { it.syncId })
    }

    @Test
    fun anEditMadeAfterADeletionBringsTheRecordBack() = runTest {
        val a = Device("a")
        val b = Device("b")
        val book = a.addBook("Dune", "epub", "cover")
        val noteId = a.database.noteDao().insert(Note(bookId = book.id, content = "first"))
        a.sync()
        b.sync()

        // B deletes the note; later A edits it. The edit is newer, so the note survives.
        b.database.noteDao().delete(b.notes().single())
        b.sync()
        Thread.sleep(5)
        a.database.noteDao().update(a.database.noteDao().getById(noteId)!!.copy(content = "kept"))
        a.sync()
        b.sync()

        assertEquals("kept", b.notes().single().content)
    }

    @Test
    fun onceTheyAgreeAnotherSyncChangesNothing() = runTest {
        val a = Device("a")
        val b = Device("b")
        val book = a.addBook("Dune", "epub", "cover")
        a.addVoiceNote(book.id, "audio")
        a.sync()
        b.sync()
        a.sync()

        val report = b.sync()

        assertEquals(SyncReport(0, 0, 0, false), report)
        assertEquals(SyncReport(0, 0, 0, false), a.sync())
    }

    @Test
    fun theCoverFileIsNamedByItsContentSoAChangedCoverIsUploadedAgain() = runTest {
        val a = Device("a")
        val b = Device("b")
        val book = a.addBook("Dune", "epub", "cover one")
        a.sync()
        b.sync()

        val newCover = a.bookFiles.newCoverFile().apply { writeText("cover two") }
        a.database.bookDao().update(book.copy(coverPath = a.bookFiles.relativePath(newCover)))
        a.sync()
        b.sync()

        val onB = b.books().single()
        assertEquals("cover two", b.file(onB.coverPath!!).readText())
        assertNotNull(newCover.sha256())
        assertEquals(1, cloud.files.keys.count { it.startsWith("cover-") })
    }
}
