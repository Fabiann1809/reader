package io.github.fabiann1809.reader.data.bookmark

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
    indices = [Index("bookId")],
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
)
