package io.github.fabiann1809.reader.data.bookmark

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookDao: BookDao
    private lateinit var bookmarkDao: BookmarkDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookDao = database.bookDao()
        bookmarkDao = database.bookmarkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertBook(title: String = "Dune") = bookDao.insert(Book(title = title, author = "Author"))

    @Test
    fun aBooksBookmarksComeInReadingOrder() = runTest {
        val bookId = insertBook()
        val otherBookId = insertBook("Cosmos")
        bookmarkDao.insert(Bookmark(bookId = bookId, location = "{}", chapter = "Final", progression = 0.9))
        bookmarkDao.insert(Bookmark(bookId = bookId, location = "{}", chapter = "Inicio", progression = 0.1))
        bookmarkDao.insert(Bookmark(bookId = otherBookId, location = "{}", chapter = "Otro libro", progression = 0.5))

        val chapters = bookmarkDao.observeByBook(bookId).first().map { it.chapter }

        assertEquals(listOf("Inicio", "Final"), chapters)
    }

    @Test
    fun deletingTheBookDeletesItsBookmarks() = runTest {
        val bookId = insertBook()
        bookmarkDao.insert(Bookmark(bookId = bookId, location = "{}", position = 3))

        bookDao.delete(bookDao.getById(bookId)!!)

        assertTrue(bookmarkDao.observeByBook(bookId).first().isEmpty())
    }

    @Test
    fun aDeletedBookmarkIsGone() = runTest {
        val bookId = insertBook()
        val id = bookmarkDao.insert(Bookmark(bookId = bookId, location = "{}"))

        bookmarkDao.delete(bookmarkDao.observeByBook(bookId).first().single { it.id == id })

        assertTrue(bookmarkDao.observeByBook(bookId).first().isEmpty())
    }
}
