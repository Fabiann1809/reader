package io.github.fabiann1809.reader.data.flashcard

import kotlinx.coroutines.flow.Flow

/** Single entry point to flashcard data. ViewModels depend on this interface so tests can use fakes. */
interface FlashcardRepository {
    /** Cards due for review at [now] (epoch milliseconds), the most overdue first. */
    fun observeDue(now: Long): Flow<List<Flashcard>>

    fun observeByBook(bookId: Long): Flow<List<Flashcard>>

    suspend fun getFlashcard(id: Long): Flashcard?

    /** When the next card is due (epoch milliseconds), of all of them; null without cards. */
    suspend fun nextReviewAt(): Long?

    /** Returns the id of the new card. */
    suspend fun addFlashcard(flashcard: Flashcard): Long

    suspend fun updateFlashcard(flashcard: Flashcard)

    suspend fun deleteFlashcard(flashcard: Flashcard)
}

class DefaultFlashcardRepository(private val flashcardDao: FlashcardDao) : FlashcardRepository {
    override fun observeDue(now: Long): Flow<List<Flashcard>> = flashcardDao.observeDue(now)

    override fun observeByBook(bookId: Long): Flow<List<Flashcard>> = flashcardDao.observeByBook(bookId)

    override suspend fun getFlashcard(id: Long): Flashcard? = flashcardDao.getById(id)

    override suspend fun nextReviewAt(): Long? = flashcardDao.nextReviewAt()

    override suspend fun addFlashcard(flashcard: Flashcard): Long = flashcardDao.insert(flashcard)

    override suspend fun updateFlashcard(flashcard: Flashcard) = flashcardDao.update(flashcard)

    override suspend fun deleteFlashcard(flashcard: Flashcard) = flashcardDao.delete(flashcard)
}
