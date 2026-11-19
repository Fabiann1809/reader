package io.github.fabiann1809.reader.data.highlight

import io.github.fabiann1809.reader.data.sync.newSyncId
import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.fabiann1809.reader.data.book.Book

/** The four highlight colors of the reader (design 2.1). */
enum class HighlightColor { YELLOW, GREEN, BLUE, PINK }

/** Text the reader highlighted in a book (T11.12): book, position, text and color (design 01 §7). */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            // Deleting a book deletes its highlights.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index(value = ["syncId"], unique = true)],
)
@Serializable
data class Highlight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    // Readium's Locator as JSON, with the highlighted text in it: Readium draws the highlight there.
    val location: String,
    val text: String,
    val color: HighlightColor = HighlightColor.YELLOW,
    // How far into the book (0 to 1): lists the highlights in reading order.
    val progression: Double? = null,
    // Epoch milliseconds.
    val createdAt: Long = System.currentTimeMillis(),
    // Identifies the record on every device (T18.1); the local `id` differs between devices.
    val syncId: String = newSyncId(),
    // Epoch milliseconds of the last change; SQLite triggers keep it up to date (see SyncTriggers).
    val updatedAt: Long = System.currentTimeMillis(),
)
