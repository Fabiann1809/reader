package io.github.fabiann1809.reader.data.book

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val currentPage: Int = 0,
    // Optional: the user may not know the page count when adding the book.
    val totalPages: Int? = null,
    val status: BookStatus = BookStatus.TO_READ,
    // Epoch milliseconds.
    val createdAt: Long = System.currentTimeMillis(),
    // Books added before version 2 were typed in by hand, so they are physical.
    @ColumnInfo(defaultValue = "PHYSICAL")
    val kind: BookKind = BookKind.PHYSICAL,
    // Null for physical books.
    val format: BookFormat? = null,
    // Copy of the book file in internal storage, relative to filesDir (see BookFiles); null for physical books.
    val filePath: String? = null,
    // Cover image in internal storage; null shows a generated cover.
    val coverPath: String? = null,
    // BCP 47 tag (e.g. "es"), when the file declares it.
    val language: String? = null,
    // Epoch milliseconds of the last time the book was opened in the reader.
    val lastOpenedAt: Long? = null,
    // Where the reader left off: Readium's Locator as JSON. Null until the book is first read.
    val readingLocation: String? = null,
    // SHA-256 of the book's file, to notice when the same file is imported again. Null for paper
    // books, and for files imported before version 8 until the app fills it in (FileHashBackfill).
    val fileHash: String? = null,
    // "Mis favoritos" is this flag (a smart collection), not a stored collection.
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    // Epoch milliseconds when it was marked finished, for "Libros terminados" of the year (T16.2).
    // Null while not finished, and for books finished before version 12.
    val finishedAt: Long? = null,
)
