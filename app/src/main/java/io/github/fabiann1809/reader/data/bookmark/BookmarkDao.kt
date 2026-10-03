package io.github.fabiann1809.reader.data.bookmark

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    /** Every row, for a backup (T17.2). */
    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<Bookmark>

    // In reading order; the oldest first between bookmarks at the same point.
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY progression ASC, createdAt ASC, id ASC")
    fun observeByBook(bookId: Long): Flow<List<Bookmark>>

    /** Returns the generated id of the new bookmark. */
    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Delete
    suspend fun delete(bookmark: Bookmark)
}
