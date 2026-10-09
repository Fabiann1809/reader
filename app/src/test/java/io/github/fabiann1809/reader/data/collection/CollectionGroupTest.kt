package io.github.fabiann1809.reader.data.collection

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionGroupTest {

    private val reading = Book(id = 1, title = "Cosmos", author = "", status = BookStatus.READING, createdAt = 1)
    private val toRead = Book(id = 2, title = "Dune", author = "", status = BookStatus.TO_READ, createdAt = 2)
    private val favorite = Book(id = 3, title = "Walden", author = "", status = BookStatus.TO_READ, isFavorite = true, createdAt = 3)

    @Test
    fun listsOnlyTheDefaultCollectionsThatHaveBooks() {
        val groups = buildCollectionGroups(listOf(reading, toRead), emptyList(), emptyList())

        assertEquals(listOf(SmartCollection.READING, SmartCollection.TO_READ), groups.map { it.smart })
        assertNull(groups.first().collection)
    }

    @Test
    fun keepsEmptyCollectionsOfTheUserAfterTheDefaultOnes() {
        val mine = Collection(id = 9, name = "Clásicos")

        val groups = buildCollectionGroups(listOf(reading), listOf(mine), emptyList())

        assertEquals(listOf(LibraryFilter.Smart(SmartCollection.READING), LibraryFilter.Custom(9)), groups.map { it.filter })
        assertEquals(emptyList<Book>(), groups.last().books)
    }

    @Test
    fun putsBooksInTheirCollectionsNewestFirst() {
        val mine = Collection(id = 9, name = "Clásicos")
        val links = listOf(BookCollectionCrossRef(bookId = 1, collectionId = 9), BookCollectionCrossRef(bookId = 3, collectionId = 9))

        val groups = buildCollectionGroups(listOf(reading, toRead, favorite), listOf(mine), links)

        assertEquals(listOf(3L, 1L), groups.last().books.map { it.id })
        assertEquals(listOf(3L), groups.first { it.smart == SmartCollection.FAVORITES }.books.map { it.id })
    }
}
