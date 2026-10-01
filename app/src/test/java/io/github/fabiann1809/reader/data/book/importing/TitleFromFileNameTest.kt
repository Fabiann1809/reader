package io.github.fabiann1809.reader.data.book.importing

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleFromFileNameTest {

    @Test
    fun dropsTheExtensionAndReadsUnderscoresAsSpaces() {
        assertEquals("el principito", titleFromFileName("el_principito.epub"))
        assertEquals("Cosmos", titleFromFileName("  Cosmos.pdf "))
    }

    @Test
    fun keepsDotsThatAreNotTheExtension() {
        assertEquals("Vol. 2 de Dune", titleFromFileName("Vol. 2 de Dune.epub"))
    }

    @Test
    fun aNameWithoutExtensionIsKept() {
        assertEquals("Cosmos", titleFromFileName("Cosmos"))
    }

    @Test
    fun noNameGivesAnEmptyTitle() {
        assertEquals("", titleFromFileName(null))
        assertEquals("", titleFromFileName(".epub"))
    }
}
