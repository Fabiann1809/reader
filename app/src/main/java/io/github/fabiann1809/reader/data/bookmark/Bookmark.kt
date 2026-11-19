package io.github.fabiann1809.reader.data.bookmark

import io.github.fabiann1809.reader.data.sync.newSyncId
import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.fabiann1809.reader.data.book.Book

/** A page the reader marked to come back to (T11.6). */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            // Deleting a book deletes its bookmarks.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId"), Index(value = ["syncId"], unique = true)],
)
@Serializable
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    // Readium's Locator as JSON: where the reader jumps back to.
    val location: String,
    // Readium's position number (about a page; a PDF's page), to know if the open page is marked.
    val position: Int? = null,
    // The chapter's title there, shown in the list; null when the book has no titles.
    val chapter: String? = null,
    // How far into the book (0 to 1): sorts the list and shows a percentage.
    val progression: Double? = null,
    // Epoch milliseconds.
    val createdAt: Long = System.currentTimeMillis(),
    // Identifies the record on every device (T18.1); the local `id` differs between devices.
    val syncId: String = newSyncId(),
    // Epoch milliseconds of the last change; SQLite triggers keep it up to date (see SyncTriggers).
    val updatedAt: Long = System.currentTimeMillis(),
)
