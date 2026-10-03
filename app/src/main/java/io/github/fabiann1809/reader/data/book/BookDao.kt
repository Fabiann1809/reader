package io.github.fabiann1809.reader.data.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /** Deletes every book, and with them (cascade) their notes, cards, sessions...: an import replaces all (T17.3). */
    @Query("DELETE FROM books")
    suspend fun deleteAll()

    /** Every row, for a backup (T17.2). */
    @Query("SELECT * FROM books")
    suspend fun getAll(): List<Book>

    // Newest first, so a freshly added book appears at the top of the library.
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?

    /** The recordings of a book's voice notes, which must be deleted with the book (its notes cascade). */
    @Query("SELECT audioPath FROM notes WHERE bookId = :bookId AND audioPath IS NOT NULL")
    suspend fun voiceNoteAudio(bookId: Long): List<String>

    @Query("SELECT * FROM books WHERE fileHash = :hash LIMIT 1")
    suspend fun findByFileHash(hash: String): Book?

    /** Returns the generated id of the new book. */
    @Insert
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)
}
