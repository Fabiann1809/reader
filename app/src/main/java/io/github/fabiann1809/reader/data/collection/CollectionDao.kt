package io.github.fabiann1809.reader.data.collection

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.fabiann1809.reader.data.book.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    /** Deletes every collection (and their links): an import replaces all (T17.3). */
    @Query("DELETE FROM collections")
    suspend fun deleteAll()

    /** Every collection, for a backup (T17.2). */
    @Query("SELECT * FROM collections")
    suspend fun getAll(): List<Collection>

    /** Every book–collection link, for a backup (T17.2). */
    @Query("SELECT * FROM book_collections")
    suspend fun getAllLinks(): List<BookCollectionCrossRef>

    @Query("SELECT * FROM book_collections")
    fun observeAllLinks(): Flow<List<BookCollectionCrossRef>>

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun count(): Int

    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Collection>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeById(id: Long): Flow<Collection?>

    /** Returns the generated id of the new collection. */
    @Insert
    suspend fun insert(collection: Collection): Long

    @Update
    suspend fun update(collection: Collection)

    /** Also removes the collection's links to books (cascade); the books stay. */
    @Delete
    suspend fun delete(collection: Collection)

    // Adding a book that is already in the collection is a no-op.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBook(link: BookCollectionCrossRef)

    @Query("DELETE FROM book_collections WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBook(bookId: Long, collectionId: Long)

    // Same order as the library: newest books first.
    @Query(
        "SELECT books.* FROM books INNER JOIN book_collections ON books.id = book_collections.bookId " +
            "WHERE book_collections.collectionId = :collectionId ORDER BY books.createdAt DESC",
    )
    fun observeBooks(collectionId: Long): Flow<List<Book>>

    @Query("SELECT collectionId FROM book_collections WHERE bookId = :bookId")
    fun observeCollectionIdsOf(bookId: Long): Flow<List<Long>>
}
