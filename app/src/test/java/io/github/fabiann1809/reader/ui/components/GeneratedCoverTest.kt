package io.github.fabiann1809.reader.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedCoverTest {

    @Test
    fun aTitleAlwaysGetsTheSameLook() {
        assertEquals(coverPattern("Cosmos"), coverPattern("Cosmos"))
        assertEquals(coverLayout("Cosmos"), coverLayout("Cosmos"))
    }

    @Test
    fun manyTitlesUseEveryPatternAndLayout() {
        val titles = (1..200).map { "Libro número $it" }

        assertEquals(CoverPattern.entries.toSet(), titles.map(::coverPattern).toSet())
        assertEquals(CoverLayout.entries.toSet(), titles.map(::coverLayout).toSet())
    }

    @Test
    fun anyTitleGetsALookEvenWithNegativeHashes() {
        listOf("", "a", "Ñandú", "📚", "x".repeat(500)).forEach {
            assertTrue(coverPattern(it) in CoverPattern.entries)
            assertTrue(coverLayout(it) in CoverLayout.entries)
        }
    }
}
