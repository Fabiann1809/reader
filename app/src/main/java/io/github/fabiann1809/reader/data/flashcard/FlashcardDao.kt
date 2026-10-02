package io.github.fabiann1809.reader.data.flashcard

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    // The most overdue first, so a short session reviews what is most likely forgotten.
    @Query("SELECT * FROM flashcards WHERE nextReviewAt <= :now ORDER BY nextReviewAt ASC, id ASC")
    fun observeDue(now: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE bookId = :bookId ORDER BY createdAt DESC, id DESC")
    fun observeByBook(bookId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getById(id: Long): Flashcard?

    /** Returns the generated id of the new card. */
    @Insert
    suspend fun insert(flashcard: Flashcard): Long

    @Update
    suspend fun update(flashcard: Flashcard)

    @Delete
    suspend fun delete(flashcard: Flashcard)
}
