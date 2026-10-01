package io.github.fabiann1809.reader.data.note

import kotlinx.coroutines.flow.Flow

/** Single entry point to note data. ViewModels depend on this interface so tests can use fakes. */
interface NoteRepository {
    fun observeNotes(bookId: Long): Flow<List<Note>>

    /** Notes of all books, newest first. */
    fun observeAllNotes(): Flow<List<Note>>

    suspend fun getNote(id: Long): Note?

    /** Returns the id of the new note. */
    suspend fun addNote(note: Note): Long

    suspend fun updateNote(note: Note)

    suspend fun deleteNote(note: Note)
}

class DefaultNoteRepository(private val noteDao: NoteDao) : NoteRepository {
    override fun observeNotes(bookId: Long): Flow<List<Note>> = noteDao.observeByBook(bookId)

    override fun observeAllNotes(): Flow<List<Note>> = noteDao.observeAll()

    override suspend fun getNote(id: Long): Note? = noteDao.getById(id)

    override suspend fun addNote(note: Note): Long = noteDao.insert(note)

    override suspend fun updateNote(note: Note) = noteDao.update(note)

    override suspend fun deleteNote(note: Note) = noteDao.delete(note)
}
