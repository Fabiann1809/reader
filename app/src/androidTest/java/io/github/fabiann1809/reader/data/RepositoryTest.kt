package io.github.fabiann1809.reader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.DefaultBookRepository
import io.github.fabiann1809.reader.data.note.DefaultNoteRepository
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var bookRepository: BookRepository
    private lateinit var noteRepository: NoteRepository
    private lateinit var filesDir: File
    private lateinit var bookFiles: BookFiles

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        // A scratch folder, so the test never touches the app's real books.
        filesDir = File(context.cacheDir, "repository-test").apply { mkdirs() }
        bookFiles = BookFiles(filesDir)
        bookRepository = DefaultBookRepository(database.bookDao(), bookFiles)
        noteRepository = DefaultNoteRepository(database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
        filesDir.deleteRecursively()
    }

    @Test
    fun insertedBookAndNoteCanBeReadBack() = runTest {
        val bookId = bookRepository.addBook(Book(title = "Cosmos", author = "Carl Sagan"))
        noteRepository.addNote(Note(bookId = bookId, content = "Somos polvo de estrellas", page = 12))

        val books = bookRepository.observeBooks().first()
        val notes = noteRepository.observeNotes(bookId).first()

        assertEquals(listOf("Cosmos"), books.map { it.title })
        assertEquals(listOf("Somos polvo de estrellas"), notes.map { it.content })
        assertEquals(12, notes.single().page)
    }

    @Test
    fun deletingAnImportedBookAlsoDeletesItsFile() = runTest {
        val file = bookFiles.newFile("epub").apply { writeText("book") }
        val id = bookRepository.addBook(Book(title = "Dune", author = "", filePath = bookFiles.relativePath(file)))

        bookRepository.deleteBook(bookRepository.getBook(id)!!)

        assertFalse(file.exists())
    }
}
