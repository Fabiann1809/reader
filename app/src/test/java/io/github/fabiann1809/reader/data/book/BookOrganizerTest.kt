package io.github.fabiann1809.reader.data.book

import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeCollectionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookOrganizerTest {

    private val books = FakeBookRepository(
        listOf(
            Book(id = 1, title = "Dune", author = "Frank Herbert", currentPage = 10, totalPages = 400),
            Book(id = 2, title = "Cosmos", author = "Carl Sagan"),
        ),
    )
    private val collections = FakeCollectionRepository(books)
    private val organizer = BookOrganizer(books, collections)

    private suspend fun booksIn(collectionId: Long) =
        collections.observeBooks(LibraryFilter.Custom(collectionId)).first().map { it.id }.toSet()

    @Test
    fun favoritesAreSetOnEveryBook() = runTest {
        organizer.setFavorite(listOf(1L, 2L), isFavorite = true)

        assertTrue(books.getBook(1)!!.isFavorite && books.getBook(2)!!.isFavorite)
    }

    @Test
    fun aBookIsAddedToAndRemovedFromACollection() = runTest {
        val collectionId = collections.createCollection("Ciencia")

        organizer.setInCollection(1, collectionId, isIncluded = true)
        assertEquals(setOf(1L), booksIn(collectionId))

        organizer.setInCollection(1, collectionId, isIncluded = false)
        assertEquals(emptySet<Long>(), booksIn(collectionId))
    }

    @Test
    fun aNewCollectionHoldsTheBooksAndABlankNameCreatesNothing() = runTest {
        val collectionId = organizer.createCollectionWith(listOf(1L, 2L), "Clásicos")!!

        assertEquals(setOf(1L, 2L), booksIn(collectionId))
        assertNull(organizer.createCollectionWith(listOf(1L), "  "))
        assertEquals(1, collections.observeCollections().first().size)
    }

    @Test
    fun markAsReadFinishesTheBook() = runTest {
        organizer.markAsRead(1)

        assertEquals(BookStatus.FINISHED, books.getBook(1)?.status)
        assertEquals(400, books.getBook(1)?.currentPage)
    }

    @Test
    fun deleteRemovesTheBooksAndIgnoresMissingOnes() = runTest {
        organizer.delete(listOf(1L, 99L))

        assertNull(books.getBook(1))
        assertEquals("Cosmos", books.getBook(2)?.title)
    }
}
