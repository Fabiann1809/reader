package io.github.fabiann1809.reader.data.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryArrangementTest {

    private val dune = Book(
        id = 1,
        title = "Dune",
        author = "Frank Herbert",
        createdAt = 1,
        lastOpenedAt = 50,
        currentPage = 10,
        totalPages = 100,
        status = BookStatus.READING,
        kind = BookKind.DIGITAL,
        format = BookFormat.EPUB,
    )
    private val angels = Book(
        id = 2,
        title = "Ángeles y demonios",
        author = "Dan Brown",
        createdAt = 2,
        currentPage = 90,
        totalPages = 100,
        status = BookStatus.FINISHED,
        kind = BookKind.DIGITAL,
        format = BookFormat.PDF,
    )
    private val cosmos = Book(id = 3, title = "cosmos", author = "Carl Sagan", createdAt = 3, lastOpenedAt = 99)
    private val books = listOf(dune, angels, cosmos)

    private fun arrange(arrangement: LibraryArrangement) = books.arrangedBy(arrangement).map { it.id }

    @Test
    fun defaultIsNewestFirstWithoutFilters() {
        assertEquals(listOf(3L, 2L, 1L), arrange(LibraryArrangement()))
        assertFalse(LibraryArrangement().hasFilters)
    }

    @Test
    fun sortsByEachOption() {
        // Never-opened books go last.
        assertEquals(listOf(3L, 1L, 2L), arrange(LibraryArrangement(sort = BookSort.LAST_READ)))
        // Accents and case do not push "Ángeles" or "cosmos" out of alphabetical order.
        assertEquals(listOf(2L, 3L, 1L), arrange(LibraryArrangement(sort = BookSort.TITLE)))
        assertEquals(listOf(3L, 2L, 1L), arrange(LibraryArrangement(sort = BookSort.AUTHOR)))
        // Unknown page count goes last.
        assertEquals(listOf(2L, 1L, 3L), arrange(LibraryArrangement(sort = BookSort.PROGRESS)))
    }

    @Test
    fun filtersAlternateWithinAGroupAndCombineAcrossGroups() {
        assertEquals(
            listOf(2L, 1L),
            arrange(LibraryArrangement(statuses = setOf(BookStatus.READING, BookStatus.FINISHED))),
        )
        assertEquals(
            listOf(1L),
            arrange(LibraryArrangement(statuses = setOf(BookStatus.READING), kinds = setOf(BookKind.DIGITAL))),
        )
        assertEquals(listOf(3L), arrange(LibraryArrangement(kinds = setOf(BookKind.PHYSICAL))))
    }

    @Test
    fun formatFilterLeavesOutPhysicalBooks() {
        assertEquals(listOf(2L), arrange(LibraryArrangement(formats = setOf(BookFormat.PDF))))
    }

    @Test
    fun withoutFiltersKeepsTheSort() {
        val arrangement = LibraryArrangement(sort = BookSort.TITLE, statuses = setOf(BookStatus.READING))

        assertTrue(arrangement.hasFilters)
        assertEquals(LibraryArrangement(sort = BookSort.TITLE), arrangement.withoutFilters())
    }
}
