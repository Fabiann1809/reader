package io.github.fabiann1809.reader.data.collection

import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.fabiann1809.reader.data.book.Book

/** A book can be in many collections and a collection holds many books. */
@Entity(
    tableName = "book_collections",
    primaryKeys = ["bookId", "collectionId"],
    foreignKeys = [
        // Deleting a book or a collection removes the link, never the other side.
        ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // bookId is covered by the primary key; collectionId needs its own index for "books in a collection".
    indices = [Index("collectionId")],
)
@Serializable
data class BookCollectionCrossRef(
    val bookId: Long,
    val collectionId: Long,
    // Epoch milliseconds.
    val addedAt: Long = System.currentTimeMillis(),
)
