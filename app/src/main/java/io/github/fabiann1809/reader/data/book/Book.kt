package io.github.fabiann1809.reader.data.book

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
)
