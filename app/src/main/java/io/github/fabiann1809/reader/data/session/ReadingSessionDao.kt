package io.github.fabiann1809.reader.data.session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {

    /** Every row, for a backup (T17.2). */
    @Query("SELECT * FROM reading_sessions")
    suspend fun getAll(): List<ReadingSession>

    /** Returns the generated id of the new session. */
    @Insert
    suspend fun insert(session: ReadingSession): Long

    // Newest first, as a book's sessions are listed.
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAt DESC")
    fun observeByBook(bookId: Long): Flow<List<ReadingSession>>

    /** Every session that started at or after [since] (epoch milliseconds), oldest first. */
    @Query("SELECT * FROM reading_sessions WHERE startedAt >= :since ORDER BY startedAt ASC")
    fun observeSince(since: Long): Flow<List<ReadingSession>>
}
