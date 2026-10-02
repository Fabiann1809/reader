package io.github.fabiann1809.reader.data.highlight

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
class HighlightDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookDao: BookDao
    private lateinit var highlightDao: HighlightDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookDao = database.bookDao()
        highlightDao = database.highlightDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertBook(title: String = "Dune") = bookDao.insert(Book(title = title, author = "Author"))

    @Test
    fun aBooksHighlightsComeInReadingOrderWithTheirColor() = runTest {
        val bookId = insertBook()
        val otherBookId = insertBook("Cosmos")
        highlightDao.insert(Highlight(bookId = bookId, location = "{}", text = "Final", progression = 0.9))
        highlightDao.insert(Highlight(bookId = bookId, location = "{}", text = "Inicio", progression = 0.1, color = HighlightColor.BLUE))
        highlightDao.insert(Highlight(bookId = otherBookId, location = "{}", text = "Otro libro"))

        val highlights = highlightDao.observeByBook(bookId).first()

        assertEquals(listOf("Inicio", "Final"), highlights.map { it.text })
        assertEquals(HighlightColor.BLUE, highlights.first().color)
    }

    @Test
    fun deletingTheBookDeletesItsHighlights() = runTest {
        val bookId = insertBook()
        highlightDao.insert(Highlight(bookId = bookId, location = "{}", text = "Algo"))

        bookDao.delete(bookDao.getById(bookId)!!)

        assertTrue(highlightDao.observeByBook(bookId).first().isEmpty())
    }
}
