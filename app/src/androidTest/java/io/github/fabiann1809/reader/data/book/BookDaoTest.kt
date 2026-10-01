package io.github.fabiann1809.reader.data.book

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookDao: BookDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookDao = database.bookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val id = bookDao.insert(Book(title = "Dune", author = "Frank Herbert", totalPages = 600))

        val book = bookDao.getById(id)

        assertEquals("Dune", book?.title)
        assertEquals(600, book?.totalPages)
        assertEquals(BookStatus.TO_READ, book?.status)
        assertEquals(BookKind.PHYSICAL, book?.kind)
    }

    @Test
    fun storesDigitalBookFileData() = runTest {
        val id = bookDao.insert(
            Book(
                title = "Cosmos",
                author = "Carl Sagan",
                kind = BookKind.DIGITAL,
                format = BookFormat.EPUB,
                filePath = "/data/books/cosmos.epub",
                coverPath = "/data/covers/cosmos.jpg",
                language = "es",
                lastOpenedAt = 5_000,
            ),
        )

        val book = bookDao.getById(id)!!

        assertEquals(BookKind.DIGITAL, book.kind)
        assertEquals(BookFormat.EPUB, book.format)
        assertEquals("/data/books/cosmos.epub", book.filePath)
        assertEquals("/data/covers/cosmos.jpg", book.coverPath)
        assertEquals("es", book.language)
        assertEquals(5_000L, book.lastOpenedAt)
    }

    @Test
    fun observeAllReturnsNewestFirst() = runTest {
        bookDao.insert(Book(title = "Old", author = "A", createdAt = 1_000))
        bookDao.insert(Book(title = "New", author = "B", createdAt = 2_000))

        val titles = bookDao.observeAll().first().map { it.title }

        assertEquals(listOf("New", "Old"), titles)
    }

    @Test
    fun updatePersistsProgressAndStatus() = runTest {
        val id = bookDao.insert(Book(title = "Dune", author = "Frank Herbert"))
        val book = bookDao.getById(id)!!

        bookDao.update(book.copy(currentPage = 42, status = BookStatus.READING))

        val updated = bookDao.observeById(id).first()
        assertEquals(42, updated?.currentPage)
        assertEquals(BookStatus.READING, updated?.status)
    }

    @Test
    fun deleteRemovesBook() = runTest {
        val id = bookDao.insert(Book(title = "Dune", author = "Frank Herbert"))

        bookDao.delete(bookDao.getById(id)!!)

        assertNull(bookDao.getById(id))
        assertEquals(emptyList<Book>(), bookDao.observeAll().first())
    }
}
