package io.github.fabiann1809.reader.data.session

import io.github.fabiann1809.reader.data.sync.newSyncId
import kotlinx.serialization.Serializable
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
    indices = [Index("bookId"), Index("startedAt"), Index(value = ["syncId"], unique = true)],
)
@Serializable
data class ReadingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    // Epoch milliseconds.
    val startedAt: Long,
    val endedAt: Long,
    val pagesRead: Int = 0,
    // Identifies the record on every device (T18.1); the local `id` differs between devices.
    val syncId: String = newSyncId(),
    // Epoch milliseconds of the last change; SQLite triggers keep it up to date (see SyncTriggers).
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val durationMillis: Long get() = endedAt - startedAt
}
