package io.github.fabiann1809.reader.data.note

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.fabiann1809.reader.data.book.Book

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            // Deleting a book deletes all of its notes.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Indexed because notes are always queried by book, and Room warns about unindexed foreign keys.
    indices = [Index("bookId")],
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val page: Int? = null,
    // Original captured text (OCR); null for manual notes.
    val sourceText: String? = null,
    // The note itself or the AI explanation.
    val content: String,
    val type: NoteType = NoteType.MANUAL,
    // Epoch milliseconds.
    val createdAt: Long = System.currentTimeMillis(),
)
