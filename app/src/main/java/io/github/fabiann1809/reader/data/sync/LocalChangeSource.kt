package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/** Tells the sync when this device's data changes, and when it last did (T18.6). */
interface LocalChangeSource {

    /** Emits after every change to the synced tables, including the ones the sync itself makes. */
    val changes: Flow<Unit>

    /** Epoch milliseconds of the newest change or deletion here; 0 when there is none. */
    suspend fun lastChangeAt(): Long
}

class RoomLocalChangeSource(private val database: AppDatabase) : LocalChangeSource {

    override val changes: Flow<Unit> = database.invalidationTracker
        .createFlow("books", "notes", "collections", "bookmarks", "highlights", "flashcards", "reading_sessions", "book_collections")
        // The first emission is the current state, not a change.
        .drop(1)
        .map { }

    override suspend fun lastChangeAt(): Long = database.syncDao().lastChangeAt() ?: 0L
}
