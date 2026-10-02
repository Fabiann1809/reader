package io.github.fabiann1809.reader.data.flashcard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookDao
import io.github.fabiann1809.reader.data.note.NoteTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var bookDao: BookDao
    private lateinit var flashcardDao: FlashcardDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookDao = database.bookDao()
        flashcardDao = database.flashcardDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertBook(title: String = "Cosmos") = bookDao.insert(Book(title = title, author = "Carl Sagan"))

    private fun card(bookId: Long, front: String, nextReviewAt: Long) =
        Flashcard(bookId = bookId, front = front, back = "Respuesta", nextReviewAt = nextReviewAt)

    @Test
    fun onlyDueCardsComeOutTheMostOverdueFirst() = runTest {
        val bookId = insertBook()
        flashcardDao.insert(card(bookId, "Mañana", nextReviewAt = 2_000))
        flashcardDao.insert(card(bookId, "Ayer", nextReviewAt = 500))
        flashcardDao.insert(card(bookId, "Hace una semana", nextReviewAt = 100))

        val due = flashcardDao.observeDue(now = 1_000).first()

        assertEquals(listOf("Hace una semana", "Ayer"), due.map { it.front })
    }

    @Test
    fun aNewCardStartsWithTheSm2DefaultsAndKeepsItsSource() = runTest {
        val bookId = insertBook()
        val id = flashcardDao.insert(
            Flashcard(bookId = bookId, page = 42, front = "¿Qué es la entropía?", back = "Una medida del desorden", tag = NoteTag.DOUBT),
        )

        val card = flashcardDao.getById(id)!!

        assertEquals(42, card.page)
        assertEquals(NoteTag.DOUBT, card.tag)
        assertEquals(Flashcard.INITIAL_EASE, card.easeFactor, 0.0)
        assertEquals(0, card.intervalDays)
        assertEquals(0, card.repetitions)
    }

    @Test
    fun deletingABookDeletesItsCards() = runTest {
        val bookId = insertBook()
        val otherBookId = insertBook("Dune")
        flashcardDao.insert(card(bookId, "Del libro", nextReviewAt = 0))
        flashcardDao.insert(card(otherBookId, "De otro libro", nextReviewAt = 0))

        bookDao.delete(bookDao.getById(bookId)!!)

        assertTrue(flashcardDao.observeByBook(bookId).first().isEmpty())
        assertEquals(listOf("De otro libro"), flashcardDao.observeDue(now = 1).first().map { it.front })
    }
}
