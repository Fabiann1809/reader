package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory NoteRepository for ViewModel tests. Mirrors the DAO ordering (newest first). */
class FakeNoteRepository(initialNotes: List<Note> = emptyList()) : NoteRepository {

    private val notes = MutableStateFlow(initialNotes)
    private var nextId = (initialNotes.maxOfOrNull { it.id } ?: 0) + 1

    val currentNotes: List<Note> get() = notes.value

    override fun observeNotes(bookId: Long): Flow<List<Note>> = notes.map { list ->
        list.filter { it.bookId == bookId }
            .sortedWith(compareByDescending<Note> { it.createdAt }.thenByDescending { it.id })
    }

    override suspend fun getNote(id: Long): Note? = notes.value.find { it.id == id }

    override suspend fun addNote(note: Note): Long {
        val id = nextId++
        notes.update { it + note.copy(id = id) }
        return id
    }

    override suspend fun updateNote(note: Note) {
        notes.update { list -> list.map { if (it.id == note.id) note else it } }
    }

    override suspend fun deleteNote(note: Note) {
        notes.update { list -> list.filterNot { it.id == note.id } }
    }
}
