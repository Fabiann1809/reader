package io.github.fabiann1809.reader.data.sync

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.note.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The real DataStore and the real Room tracker behind the background sync (T18.6). */
@RunWith(AndroidJUnit4::class)
class SyncSupportTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // One file per test: DataStore refuses two live stores on the same file in one process.
    private val testFile = "sync_settings_test_${System.nanoTime()}"

    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .addCallback(SyncTriggersCallback)
        .build()

    @After
    fun tearDown() {
        database.close()
        context.preferencesDataStoreFile(testFile).delete()
    }

    @Test
    fun aNewSettingsStoreIsOffAndWaitsForWifi() = runTest {
        assertEquals(SyncSettings(enabled = false, wifiOnly = true, lastSyncAt = null), DataStoreSyncSettingsStore(context, testFile).settings.first())
    }

    @Test
    fun savedSettingsComeBackAndTheLastSyncCanBeCleared() = runTest {
        val store = DataStoreSyncSettingsStore(context, testFile)

        store.update { it.copy(enabled = true, wifiOnly = false, lastSyncAt = 1234) }
        assertEquals(SyncSettings(enabled = true, wifiOnly = false, lastSyncAt = 1234), store.settings.first())

        store.update { it.copy(lastSyncAt = null) }
        assertNull(store.settings.first().lastSyncAt)
    }

    @Test
    // Real time (runBlocking): the waits below must really happen, runTest would skip them.
    fun theChangeSourceStaysQuietUntilTheDataChanges() = runBlocking {
        val source = RoomLocalChangeSource(database)

        val beforeAnyChange = withTimeoutOrNull(300) { source.changes.first() }
        assertNull(beforeAnyChange)

        // A change arrives through the same flow once something is written.
        val change = async(Dispatchers.IO) { withTimeoutOrNull(5_000) { source.changes.first() } }
        delay(300)
        database.bookDao().insert(Book(title = "Dune", author = "Herbert"))

        assertNotNull(change.await())
    }

    @Test
    fun theLastChangeIsTheNewestEditOrDeletion() = runTest {
        val source = RoomLocalChangeSource(database)
        assertEquals(0L, source.lastChangeAt())

        val bookId = database.bookDao().insert(Book(title = "Dune", author = "Herbert", updatedAt = 1_000))
        assertEquals(1_000L, source.lastChangeAt())

        val noteId = database.noteDao().insert(Note(bookId = bookId, content = "x", updatedAt = 5_000))
        assertEquals(5_000L, source.lastChangeAt())

        // A deletion counts too: its tombstone is the newest thing here.
        database.noteDao().delete(database.noteDao().getById(noteId)!!)
        assertTrue(source.lastChangeAt() > 5_000L)
    }
}
