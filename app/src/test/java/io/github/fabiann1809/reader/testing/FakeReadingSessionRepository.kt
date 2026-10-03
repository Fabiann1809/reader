package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.data.session.ReadingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReadingSessionRepository(initial: List<ReadingSession> = emptyList()) : ReadingSessionRepository {
    private val sessions = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val currentSessions: List<ReadingSession> get() = sessions.value

    override suspend fun addSession(session: ReadingSession): Long {
        val id = nextId++
        sessions.update { it + session.copy(id = id) }
        return id
    }

    override fun observeByBook(bookId: Long): Flow<List<ReadingSession>> =
        sessions.map { list -> list.filter { it.bookId == bookId }.sortedByDescending { it.startedAt } }

    override fun observeSince(since: Long): Flow<List<ReadingSession>> =
        sessions.map { list -> list.filter { it.startedAt >= since }.sortedBy { it.startedAt } }
}
