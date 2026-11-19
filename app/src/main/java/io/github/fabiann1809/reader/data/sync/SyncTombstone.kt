package io.github.fabiann1809.reader.data.sync

import androidx.room.Entity

/**
 * Remembers that a record was deleted, so the other devices delete it too instead of sending it
 * back. SQLite triggers fill it (see [SyncTriggers]); nothing in the app writes it directly.
 */
@Entity(tableName = "sync_tombstones", primaryKeys = ["entityType", "syncId"])
data class SyncTombstone(
    // One of the type names in [SyncTriggers], e.g. "book".
    val entityType: String,
    // The deleted record's syncId; for a book–collection link, "bookSyncId|collectionSyncId".
    val syncId: String,
    // Epoch milliseconds.
    val deletedAt: Long,
)
