package io.github.fabiann1809.reader.data.flashcard

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.note.NoteTag

/**
 * A card to review (T14.1): a [front] (question or concept) and a [back] (answer or explanation),
 * from a book and optionally a page (design 01 §4.8). The spaced repetition fields follow a
 * simplified SM-2 (T14.4): the card is due at [nextReviewAt], [intervalDays] after the last review.
 */
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            // Deleting a book deletes its cards, like its notes.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("nextReviewAt")],
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val page: Int? = null,
    val front: String,
    val back: String,
    // The label of the note it came from, for "Por etiqueta" in the Repasar tab (T14.3).
    val tag: NoteTag? = null,
    // Epoch milliseconds; a new card is due at once.
    val nextReviewAt: Long = System.currentTimeMillis(),
    // SM-2's ease: how fast the interval grows (2.5 for a new card, never below 1.3).
    val easeFactor: Double = INITIAL_EASE,
    val intervalDays: Int = 0,
    // Correct reviews in a row; SM-2 restarts it on a miss.
    val repetitions: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val INITIAL_EASE = 2.5
    }
}
