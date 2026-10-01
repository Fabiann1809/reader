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
    // Copy of the book file in the app's internal storage; null for physical books.
    val filePath: String? = null,
    // Cover image in internal storage; null shows a generated cover.
    val coverPath: String? = null,
    // BCP 47 tag (e.g. "es"), when the file declares it.
    val language: String? = null,
    // Epoch milliseconds of the last time the book was opened in the reader.
    val lastOpenedAt: Long? = null,
    // "Mis favoritos" is this flag (a smart collection), not a stored collection.
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
)
