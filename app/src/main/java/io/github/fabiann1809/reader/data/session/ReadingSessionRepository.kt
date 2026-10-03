package io.github.fabiann1809.reader.data.session

import kotlinx.coroutines.flow.Flow

/** Single entry point to reading sessions. ViewModels depend on this interface so tests can use fakes. */
interface ReadingSessionRepository {
    suspend fun addSession(session: ReadingSession): Long

    fun observeByBook(bookId: Long): Flow<List<ReadingSession>>

    /** Sessions that started at or after [since] (epoch milliseconds), oldest first. */
    fun observeSince(since: Long): Flow<List<ReadingSession>>
}

class DefaultReadingSessionRepository(private val dao: ReadingSessionDao) : ReadingSessionRepository {
    override suspend fun addSession(session: ReadingSession): Long = dao.insert(session)

    override fun observeByBook(bookId: Long): Flow<List<ReadingSession>> = dao.observeByBook(bookId)

    override fun observeSince(since: Long): Flow<List<ReadingSession>> = dao.observeSince(since)
}
