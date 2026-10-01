package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.reader.TocEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderContentsTest {

    private val bookmarks = listOf(
        Bookmark(id = 1, bookId = 1, location = "{}", position = 4),
        Bookmark(id = 2, bookId = 1, location = "{}", position = 9),
    )

    @Test
    fun theBookmarksOfThePageShareItsPosition() {
        assertEquals(listOf(1L), bookmarks.atPosition(4).map { it.id })
        assertTrue(bookmarks.atPosition(5).isEmpty())
        assertTrue(bookmarks.atPosition(null).isEmpty())
    }

    @Test
    fun theOpenEntryIsTheChapterOfTheFile() {
        val toc = listOf(
            TocEntry(index = 0, title = "Capítulo 1", level = 0, href = "c1.xhtml"),
            TocEntry(index = 1, title = "Capítulo 2", level = 0, href = "c2.xhtml"),
            TocEntry(index = 2, title = "Sección 2.1", level = 1, href = "c2.xhtml"),
        )

        assertEquals(1, toc.openEntry("c2.xhtml")?.index)
        assertEquals(0, toc.openEntry("c1.xhtml")?.index)
        assertNull(toc.openEntry("otro.xhtml"))
        assertNull(toc.openEntry(null))
    }
}
