package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSearchTest {

    private val book = Book(title = "Cien años de soledad", author = "Gabriel García Márquez")

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(book.matchesSearch(""))
        assertTrue(book.matchesSearch("   "))
    }

    @Test
    fun matchesTitleOrAuthorIgnoringCaseAndAccents() {
        assertTrue(book.matchesSearch("SOLEDAD"))
        assertTrue(book.matchesSearch("garcia"))
        assertTrue(book.matchesSearch("Años"))
        assertTrue(book.matchesSearch("anos"))
    }

    @Test
    fun everyWordMustMatchButNotInOrder() {
        assertTrue(book.matchesSearch("márquez cien"))
        assertFalse(book.matchesSearch("márquez dune"))
    }
}
