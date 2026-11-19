package io.github.fabiann1809.reader.data.sync

import androidx.room.withTransaction
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord

/** [LocalSyncData] over Room and the app's files: it reads with [LocalSnapshotReader] and writes with [LocalChangeApplier]. */
class RoomLocalSyncData(
    private val database: AppDatabase,
    private val reader: LocalSnapshotReader,
    private val applier: LocalChangeApplier,
) : LocalSyncData {

    override suspend fun snapshot(): LocalSnapshot = reader.read()

    override suspend fun rename(renames: Map<RecordKey, String>) {
        val dao = database.syncDao()
        database.withTransaction {
            renames.forEach { (key, newSyncId) ->
                when (key.type) {
                    SyncTypes.BOOK -> dao.renameBook(key.syncId, newSyncId)
                    SyncTypes.COLLECTION -> dao.renameCollection(key.syncId, newSyncId)
                }
            }
        }
    }

    override suspend fun apply(toApply: List<SyncRecord>, toDelete: List<RecordKey>, downloader: FileDownloader) =
        applier.apply(toApply, toDelete, downloader)
}
