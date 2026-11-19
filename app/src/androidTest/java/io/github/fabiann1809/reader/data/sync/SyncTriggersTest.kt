package io.github.fabiann1809.reader.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.session.ReadingSession
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The sync bookkeeping the triggers do by themselves (T18.1): updatedAt, tombstones and undo. */
@RunWith(AndroidJUnit4::class)
class SyncTriggersTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SyncTriggersCallback)
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val bookDao get() = database.bookDao()
    private val noteDao get() = database.noteDao()
    private val collectionDao get() = database.collectionDao()
    private suspend fun tombstones() = database.syncTombstoneDao().getAll()

    private suspend fun insertBook(updatedAt: Long = OLD) =
        bookDao.insert(Book(title = "Dune", author = "Herbert", updatedAt = updatedAt)).let { bookDao.getById(it)!! }

    @Test
    fun everyRecordGetsItsOwnSyncId() = runTest {
        val first = insertBook()
        val second = insertBook()

        assertTrue(first.syncId.isNotBlank())
        assertNotEquals(first.syncId, second.syncId)
    }

    @Test
    fun updatingARecordMovesUpdatedAtToNow() = runTest {
        val book = insertBook()

        bookDao.update(book.copy(title = "Dune Messiah"))

        assertTrue(bookDao.getById(book.id)!!.updatedAt > OLD)
    }

    @Test
    fun updatingFromAStaleCopyNeverMovesUpdatedAtBack() = runTest {
        val stale = insertBook()
        bookDao.update(stale.copy(title = "First change"))
        val afterFirst = bookDao.getById(stale.id)!!.updatedAt

        bookDao.update(stale.copy(title = "Second change"))

        assertTrue(bookDao.getById(stale.id)!!.updatedAt >= afterFirst)
    }

    @Test
    fun aNewerUpdatedAtIsKeptAsIs() = runTest {
        val book = insertBook()
        val fromAnotherDevice = FUTURE

        bookDao.update(book.copy(title = "Edited elsewhere", updatedAt = fromAnotherDevice))

        assertEquals(fromAnotherDevice, bookDao.getById(book.id)!!.updatedAt)
    }

    @Test
    fun deletingARecordLeavesATombstone() = runTest {
        val book = insertBook()

        bookDao.delete(book)

        val tombstone = tombstones().single()
        assertEquals("book", tombstone.entityType)
        assertEquals(book.syncId, tombstone.syncId)
        assertTrue(tombstone.deletedAt > OLD)
    }

    @Test
    fun deletingABookLeavesTombstonesForWhatWentWithIt() = runTest {
        val book = insertBook()
        val noteId = noteDao.insert(Note(bookId = book.id, content = "Idea"))
        val note = noteDao.getById(noteId)!!
        database.readingSessionDao().insert(ReadingSession(bookId = book.id, startedAt = 1, endedAt = 2))

        bookDao.delete(book)

        val byType = tombstones().groupBy { it.entityType }
        assertEquals(note.syncId, byType.getValue("note").single().syncId)
        assertEquals(1, byType.getValue("reading_session").size)
        assertEquals(1, byType.getValue("book").size)
    }

    @Test
    fun undoingADeletionDropsTheTombstoneAndWinsOverTheDeletion() = runTest {
        val book = insertBook()
        val noteId = noteDao.insert(Note(bookId = book.id, content = "Idea", updatedAt = OLD))
        val note = noteDao.getById(noteId)!!
        noteDao.delete(note)
        val deletedAt = tombstones().single().deletedAt

        // The undo snackbar puts the same note back, with its old values.
        noteDao.insert(note)

        assertTrue(tombstones().isEmpty())
        assertTrue(noteDao.getById(note.id)!!.updatedAt >= deletedAt)
    }

    @Test
    fun removingABookFromACollectionLeavesATombstoneNamedByBothSyncIds() = runTest {
        val book = insertBook()
        val collectionId = collectionDao.insert(Collection(name = "Clásicos"))
        val collection = collectionDao.getAll().single { it.id == collectionId }
        collectionDao.addBook(BookCollectionCrossRef(book.id, collectionId))

        collectionDao.removeBook(book.id, collectionId)

        val tombstone = tombstones().single()
        assertEquals(SyncTriggers.LINK_TYPE, tombstone.entityType)
        assertEquals("${book.syncId}|${collection.syncId}", tombstone.syncId)
    }

    @Test
    fun addingTheLinkAgainDropsItsTombstone() = runTest {
        val book = insertBook()
        val collectionId = collectionDao.insert(Collection(name = "Clásicos"))
        collectionDao.addBook(BookCollectionCrossRef(book.id, collectionId, addedAt = OLD))
        collectionDao.removeBook(book.id, collectionId)
        val deletedAt = tombstones().single().deletedAt

        collectionDao.addBook(BookCollectionCrossRef(book.id, collectionId, addedAt = OLD))

        assertTrue(tombstones().isEmpty())
        assertTrue(collectionDao.getAllLinks().single().addedAt >= deletedAt)
    }

    private companion object {
        const val OLD = 1_000L
        const val FUTURE = 4_000_000_000_000L
    }
}
