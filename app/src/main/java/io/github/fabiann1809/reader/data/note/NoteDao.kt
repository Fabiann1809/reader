package io.github.fabiann1809.reader.data.note

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // Newest first; id breaks ties between notes created in the same millisecond.
    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY createdAt DESC, id DESC")
    fun observeByBook(bookId: Long): Flow<List<Note>>

    // Every note of every book, newest first.
    @Query("SELECT * FROM notes ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    /** Returns the generated id of the new note. */
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}
