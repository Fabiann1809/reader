package io.github.fabiann1809.reader.data.sync

import androidx.room.withTransaction
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.voice.VoiceFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Empties the library of this device (T18.8). Only used once everything is safe in the cloud. */
interface LocalDataEraser {
    suspend fun eraseAll()
}

class RoomLocalDataEraser(
    private val database: AppDatabase,
    private val bookFiles: BookFiles,
    private val voiceFiles: VoiceFiles,
) : LocalDataEraser {

    override suspend fun eraseAll() = withContext(Dispatchers.IO) {
        val dao = database.syncDao()
        database.withTransaction {
            dao.deleteAllBooks()
            dao.deleteAllCollections()
            // The deletions above left tombstones; they are not real deletions to tell the cloud about.
            dao.deleteAllTombstones()
        }
        bookFiles.deleteAll()
        voiceFiles.deleteAll()
    }
}
