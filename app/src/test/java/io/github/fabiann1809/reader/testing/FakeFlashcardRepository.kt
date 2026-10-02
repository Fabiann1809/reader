package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory FlashcardRepository for ViewModel tests. Mirrors the DAO ordering. */
class FakeFlashcardRepository(initialCards: List<Flashcard> = emptyList()) : FlashcardRepository {

    private val cards = MutableStateFlow(initialCards)
    private var nextId = (initialCards.maxOfOrNull { it.id } ?: 0) + 1

    val currentCards: List<Flashcard> get() = cards.value

    override fun observeDue(now: Long): Flow<List<Flashcard>> = cards.map { list ->
        list.filter { it.nextReviewAt <= now }.sortedWith(compareBy<Flashcard> { it.nextReviewAt }.thenBy { it.id })
    }

    override fun observeByBook(bookId: Long): Flow<List<Flashcard>> = cards.map { list ->
        list.filter { it.bookId == bookId }.sortedWith(compareByDescending<Flashcard> { it.createdAt }.thenByDescending { it.id })
    }

    override suspend fun getFlashcard(id: Long): Flashcard? = cards.value.find { it.id == id }

    override suspend fun nextReviewAt(): Long? = cards.value.minOfOrNull { it.nextReviewAt }

    override suspend fun addFlashcard(flashcard: Flashcard): Long {
        val id = nextId++
        cards.update { it + flashcard.copy(id = id) }
        return id
    }

    override suspend fun updateFlashcard(flashcard: Flashcard) {
        cards.update { list -> list.map { if (it.id == flashcard.id) flashcard else it } }
    }

    override suspend fun deleteFlashcard(flashcard: Flashcard) {
        cards.update { list -> list.filterNot { it.id == flashcard.id } }
    }
}
