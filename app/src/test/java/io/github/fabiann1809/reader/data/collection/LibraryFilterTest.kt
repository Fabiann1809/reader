package io.github.fabiann1809.reader.data.collection

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {

    @Test
    fun encodedFiltersDecodeBack() {
        val filters = SmartCollection.entries.map { LibraryFilter.Smart(it) } + LibraryFilter.Custom(12)

        filters.forEach { assertEquals(it, LibraryFilter.decode(it.encode())) }
    }

    @Test
    fun unknownValuesFallBackToAll() {
        listOf(null, "", "smart:NOPE", "custom:abc", "other:1", "garbage").forEach {
            assertEquals(LibraryFilter.Default, LibraryFilter.decode(it))
        }
    }
}
