package io.github.fabiann1809.reader.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.note.NoteType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrades a database created with the first exported schema (what users already have installed)
 * to the current version, and checks that books and notes survive. Each new migration added to
 * [ALL_MIGRATIONS] is covered here automatically; add data for any new columns it touches.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun upgradesFromVersion1KeepingBooksAndNotes() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO books (id, title, author, currentPage, totalPages, status, createdAt) " +
                    "VALUES (1, 'Cosmos', 'Carl Sagan', 120, 400, 'READING', 1000)",
            )
            execSQL(
                "INSERT INTO notes (id, bookId, page, sourceText, content, type, createdAt) " +
                    "VALUES (1, 1, 12, 'Texto original', 'Idea central', 'EXPLANATION', 2000)",
            )
            close()
        }

        // Opening through Room runs every migration and validates the result against the current schema.
        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val book = database.bookDao().observeAll().first().single()
            assertEquals("Cosmos", book.title)
            assertEquals("Carl Sagan", book.author)
            assertEquals(120, book.currentPage)
            assertEquals(400, book.totalPages)
            assertEquals(BookStatus.READING, book.status)
            // Added in version 2: books typed in by hand become physical books with no file.
            assertEquals(BookKind.PHYSICAL, book.kind)
            assertNull(book.format)
            assertNull(book.filePath)
            assertNull(book.coverPath)
            assertNull(book.language)
            assertNull(book.lastOpenedAt)
            // Added in version 3: nothing is a favorite and there are no user collections yet.
            assertFalse(book.isFavorite)
            assertTrue(database.collectionDao().observeAll().first().isEmpty())
            // Added in version 4: the book was never opened in the reader.
            assertNull(book.readingLocation)
            // Added in version 8: filled in by FileHashBackfill when the app starts (paper books keep null).
            assertNull(book.fileHash)
            // Added in version 5: no bookmarks yet.
            assertTrue(database.bookmarkDao().observeByBook(1).first().isEmpty())
            // Added in version 6: no highlights yet.
            assertTrue(database.highlightDao().observeByBook(1).first().isEmpty())

            val note = database.noteDao().observeByBook(1).first().single()
            assertEquals("Idea central", note.content)
            assertEquals("Texto original", note.sourceText)
            assertEquals(12, note.page)
            assertEquals(NoteType.EXPLANATION, note.type)
            // Added in version 7: the note isn't tied to a place in the book.
            assertNull(note.location)
            // Added in version 9: not a voice note.
            assertNull(note.audioPath)
            assertNull(note.tag)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
