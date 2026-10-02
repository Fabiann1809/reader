package io.github.fabiann1809.reader.data.highlight

import kotlinx.coroutines.flow.Flow

/** Single entry point to highlights. ViewModels depend on this interface so tests can use fakes. */
interface HighlightRepository {
    /** The highlights of [bookId], in reading order. */
    fun observeHighlights(bookId: Long): Flow<List<Highlight>>

    /** Returns the id of the new highlight. */
    suspend fun addHighlight(highlight: Highlight): Long

    suspend fun deleteHighlight(highlight: Highlight)
}

class DefaultHighlightRepository(private val highlightDao: HighlightDao) : HighlightRepository {
    override fun observeHighlights(bookId: Long): Flow<List<Highlight>> = highlightDao.observeByBook(bookId)

    override suspend fun addHighlight(highlight: Highlight): Long = highlightDao.insert(highlight)

    override suspend fun deleteHighlight(highlight: Highlight) = highlightDao.delete(highlight)
}
