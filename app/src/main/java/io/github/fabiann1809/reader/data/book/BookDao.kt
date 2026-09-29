package io.github.fabiann1809.reader.data.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // Newest first, so a freshly added book appears at the top of the library.
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?

    /** Returns the generated id of the new book. */
    @Insert
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)
}
