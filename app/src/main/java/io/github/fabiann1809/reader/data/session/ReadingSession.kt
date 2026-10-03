package io.github.fabiann1809.reader.data.session

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.fabiann1809.reader.data.book.Book

/**
 * A stretch of reading (T16.1, design 01 §8: book, start, end, pages read). From the reader it spans
 * the time the book was open; for a paper book, updating its progress records the pages read, with
 * no duration ([startedAt] equals [endedAt]).
 */
@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            // Deleting a book deletes its sessions.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index("startedAt")],
)
data class ReadingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    // Epoch milliseconds.
    val startedAt: Long,
    val endedAt: Long,
    val pagesRead: Int = 0,
) {
    val durationMillis: Long get() = endedAt - startedAt
}
