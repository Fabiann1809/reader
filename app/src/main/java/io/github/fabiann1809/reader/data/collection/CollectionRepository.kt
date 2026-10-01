package io.github.fabiann1809.reader.data.collection

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single entry point to collections. ViewModels depend on this interface so tests can use fakes. */
interface CollectionRepository {
    /** Collections created by the user, sorted by name. */
    fun observeCollections(): Flow<List<Collection>>

    fun observeCollection(id: Long): Flow<Collection?>

    /** Books shown for [filter], newest first. */
    fun observeBooks(filter: LibraryFilter): Flow<List<Book>>

    /** Ids of the user collections that contain the book. */
    fun observeCollectionIdsOf(bookId: Long): Flow<List<Long>>

    /** Returns the id of the new collection. */
    suspend fun createCollection(name: String): Long

    suspend fun renameCollection(collection: Collection, name: String)

    suspend fun deleteCollection(collection: Collection)

    suspend fun addBook(bookId: Long, collectionId: Long)

    suspend fun removeBook(bookId: Long, collectionId: Long)
}

class DefaultCollectionRepository(
    private val collectionDao: CollectionDao,
    private val bookDao: BookDao,
) : CollectionRepository {

    override fun observeCollections(): Flow<List<Collection>> = collectionDao.observeAll()

    override fun observeCollection(id: Long): Flow<Collection?> = collectionDao.observeById(id)

    override fun observeBooks(filter: LibraryFilter): Flow<List<Book>> = when (filter) {
        is LibraryFilter.Smart -> bookDao.observeAll().map { books -> books.filter(filter.collection.includes) }
        is LibraryFilter.Custom -> collectionDao.observeBooks(filter.collectionId)
    }

    override fun observeCollectionIdsOf(bookId: Long): Flow<List<Long>> = collectionDao.observeCollectionIdsOf(bookId)

    override suspend fun createCollection(name: String): Long = collectionDao.insert(Collection(name = name.trim()))

    override suspend fun renameCollection(collection: Collection, name: String) =
        collectionDao.update(collection.copy(name = name.trim()))

    override suspend fun deleteCollection(collection: Collection) = collectionDao.delete(collection)

    override suspend fun addBook(bookId: Long, collectionId: Long) =
        collectionDao.addBook(BookCollectionCrossRef(bookId = bookId, collectionId = collectionId))

    override suspend fun removeBook(bookId: Long, collectionId: Long) = collectionDao.removeBook(bookId, collectionId)
}
