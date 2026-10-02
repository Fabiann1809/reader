package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory HighlightRepository for ViewModel tests. Mirrors the DAO ordering (reading order). */
class FakeHighlightRepository(initialHighlights: List<Highlight> = emptyList()) : HighlightRepository {

    private val highlights = MutableStateFlow(initialHighlights)
    private var nextId = (initialHighlights.maxOfOrNull { it.id } ?: 0) + 1

    val currentHighlights: List<Highlight> get() = highlights.value

    override fun observeHighlights(bookId: Long): Flow<List<Highlight>> = highlights.map { list ->
        list.filter { it.bookId == bookId }
            .sortedWith(compareBy<Highlight> { it.progression }.thenBy { it.createdAt }.thenBy { it.id })
    }

    override suspend fun addHighlight(highlight: Highlight): Long {
        val id = nextId++
        highlights.update { it + highlight.copy(id = id) }
        return id
    }

    override suspend fun deleteHighlight(highlight: Highlight) {
        highlights.update { list -> list.filterNot { it.id == highlight.id } }
    }
}
