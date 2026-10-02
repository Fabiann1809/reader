package io.github.fabiann1809.reader.data.highlight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    // In reading order; the oldest first between highlights at the same point.
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY progression ASC, createdAt ASC, id ASC")
    fun observeByBook(bookId: Long): Flow<List<Highlight>>

    /** Returns the generated id of the new highlight. */
    @Insert
    suspend fun insert(highlight: Highlight): Long

    @Delete
    suspend fun delete(highlight: Highlight)
}
