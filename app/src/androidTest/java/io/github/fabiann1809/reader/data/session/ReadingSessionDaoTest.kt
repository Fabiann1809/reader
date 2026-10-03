package io.github.fabiann1809.reader.data.session

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingSessionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ReadingSessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.readingSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sessionsAreListedByBookAndSinceADay() = runTest {
        val cosmos = database.bookDao().insert(Book(title = "Cosmos", author = "Carl Sagan"))
        val dune = database.bookDao().insert(Book(title = "Dune", author = "Frank Herbert"))
        dao.insert(ReadingSession(bookId = cosmos, startedAt = 1_000, endedAt = 61_000, pagesRead = 3))
        dao.insert(ReadingSession(bookId = cosmos, startedAt = 5_000, endedAt = 9_000, pagesRead = 1))
        dao.insert(ReadingSession(bookId = dune, startedAt = 3_000, endedAt = 4_000))

        assertEquals(listOf(5_000L, 1_000L), dao.observeByBook(cosmos).first().map { it.startedAt })
        assertEquals(listOf(3_000L, 5_000L), dao.observeSince(2_000).first().map { it.startedAt })
    }

    @Test
    fun deletingABookDeletesItsSessions() = runTest {
        val bookId = database.bookDao().insert(Book(title = "Cosmos", author = "Carl Sagan"))
        dao.insert(ReadingSession(bookId = bookId, startedAt = 1_000, endedAt = 2_000))

        database.bookDao().delete(database.bookDao().getById(bookId)!!)

        assertTrue(dao.observeByBook(bookId).first().isEmpty())
    }
}
