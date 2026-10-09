package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.COLOR_COUNT
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory CollectionRepository that reads books from [books], mirroring the real ordering rules. */
class FakeCollectionRepository(private val books: FakeBookRepository) : CollectionRepository {

    private val collections = MutableStateFlow<List<Collection>>(emptyList())
    private val links = MutableStateFlow<Set<Pair<Long, Long>>>(emptySet())
    private var nextId = 1L

    override fun observeCollections(): Flow<List<Collection>> =
        collections.map { list -> list.sortedBy { it.name.lowercase() } }

    override fun observeCollection(id: Long): Flow<Collection?> = collections.map { list -> list.find { it.id == id } }

    override fun observeBooks(filter: LibraryFilter): Flow<List<Book>> =
        combine(books.observeBooks(), links) { all, links ->
            when (filter) {
                is LibraryFilter.Smart -> all.filter(filter.collection.includes)
                is LibraryFilter.Custom -> all.filter { (it.id to filter.collectionId) in links }
            }
        }

    override fun observeCollectionIdsOf(bookId: Long): Flow<List<Long>> =
        links.map { set -> set.filter { it.first == bookId }.map { it.second } }

    override fun observeLinks(): Flow<List<BookCollectionCrossRef>> =
        links.map { set -> set.map { (bookId, collectionId) -> BookCollectionCrossRef(bookId, collectionId, addedAt = 0) } }

    override suspend fun createCollection(name: String, colorIndex: Int?): Long {
        val id = nextId++
        collections.update { it + Collection(id = id, name = name.trim(), colorIndex = colorIndex ?: (it.size % COLOR_COUNT)) }
        return id
    }

    override suspend fun renameCollection(collection: Collection, name: String) {
        collections.update { list -> list.map { if (it.id == collection.id) it.copy(name = name.trim()) else it } }
    }

    override suspend fun deleteCollection(collection: Collection) {
        collections.update { list -> list.filterNot { it.id == collection.id } }
        links.update { set -> set.filterNot { it.second == collection.id }.toSet() }
    }

    override suspend fun addBook(bookId: Long, collectionId: Long) {
        links.update { it + (bookId to collectionId) }
    }

    override suspend fun removeBook(bookId: Long, collectionId: Long) {
        links.update { it - (bookId to collectionId) }
    }
}
