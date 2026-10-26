package io.github.fabiann1809.reader.data.book.lookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsbnTest {

    @Test
    fun acceptsAValidIsbn13WithHyphensAndSpaces() {
        assertEquals("9780306406157", Isbn.normalize("978-0-306-40615-7"))
        assertEquals("9780306406157", Isbn.normalize(" 978 0306406157 "))
    }

    @Test
    fun acceptsAValidIsbn10IncludingTheXCheckDigit() {
        assertEquals("0306406152", Isbn.normalize("0-306-40615-2"))
        assertEquals("080442957X", Isbn.normalize("0-8044-2957-x"))
    }

    @Test
    fun rejectsAWrongCheckDigitOrLength() {
        assertNull(Isbn.normalize("9780306406158"))
        assertNull(Isbn.normalize("0306406153"))
        assertNull(Isbn.normalize("12345"))
        assertNull(Isbn.normalize(""))
    }

    @Test
    fun rejectsLettersInsideTheNumber() {
        assertNull(Isbn.normalize("97803064X6157"))
    }

    @Test
    fun readsTitleAuthorsAndPagesFromOpenLibrary() {
        val body = """{"ISBN:9780306406157": {"title": " Cosmos ", "authors": [{"name": "Carl Sagan"}, {"name": "Ann Druyan"}], "number_of_pages": 365}}"""

        val result = parseOpenLibrary(body, "9780306406157")

        assertEquals(IsbnResult.Found(BookInfo("Cosmos", "Carl Sagan, Ann Druyan", 365)), result)
    }

    @Test
    fun aBookWithoutAuthorsOrPagesStillFills() {
        val body = """{"ISBN:1": {"title": "Cosmos", "number_of_pages": 0}}"""

        assertEquals(IsbnResult.Found(BookInfo("Cosmos", "", null)), parseOpenLibrary(body, "1"))
    }

    @Test
    fun anUnknownIsbnOrAnUnreadableAnswerIsNotFound() {
        assertEquals(IsbnResult.NotFound, parseOpenLibrary("{}", "1"))
        assertEquals(IsbnResult.NotFound, parseOpenLibrary("<html>", "1"))
    }
}
