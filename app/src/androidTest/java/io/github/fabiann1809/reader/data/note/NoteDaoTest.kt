package io.github.fabiann1809.reader.data.note

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookDao: BookDao
    private lateinit var noteDao: NoteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookDao = database.bookDao()
        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertBook(title: String = "Dune") =
        bookDao.insert(Book(title = title, author = "Author"))

    @Test
    fun insertAndGetById() = runTest {
        val bookId = insertBook()
        val id = noteDao.insert(
            Note(
                bookId = bookId,
                page = 7,
                sourceText = "Original text",
                content = "Explanation",
                type = NoteType.EXPLANATION,
            ),
        )

        val note = noteDao.getById(id)

        assertEquals("Explanation", note?.content)
        assertEquals("Original text", note?.sourceText)
        assertEquals(NoteType.EXPLANATION, note?.type)
    }

    @Test
    fun observeByBookReturnsOnlyThatBooksNotesNewestFirst() = runTest {
        val bookId = insertBook("Dune")
        val otherBookId = insertBook("Cosmos")
        noteDao.insert(Note(bookId = bookId, content = "First", createdAt = 1_000))
        noteDao.insert(Note(bookId = bookId, content = "Second", createdAt = 2_000))
        noteDao.insert(Note(bookId = otherBookId, content = "Other book"))

        val contents = noteDao.observeByBook(bookId).first().map { it.content }

        assertEquals(listOf("Second", "First"), contents)
    }

    @Test
    fun updatePersistsContent() = runTest {
        val id = noteDao.insert(Note(bookId = insertBook(), content = "Draft"))

        noteDao.update(noteDao.getById(id)!!.copy(content = "Final"))

        assertEquals("Final", noteDao.getById(id)?.content)
    }

    @Test
    fun deleteRemovesNote() = runTest {
        val id = noteDao.insert(Note(bookId = insertBook(), content = "Temp"))

        noteDao.delete(noteDao.getById(id)!!)

        assertNull(noteDao.getById(id))
    }

    @Test
    fun deletingBookDeletesItsNotes() = runTest {
        val bookId = insertBook()
        val noteId = noteDao.insert(Note(bookId = bookId, content = "Will be deleted"))

        bookDao.delete(bookDao.getById(bookId)!!)

        assertNull(noteDao.getById(noteId))
    }

    @Test
    fun insertingNoteForMissingBookFails() = runTest {
        val result = runCatching { noteDao.insert(Note(bookId = 999, content = "Orphan")) }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }
}
