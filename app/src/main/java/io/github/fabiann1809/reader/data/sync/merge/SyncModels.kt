package io.github.fabiann1809.reader.data.sync.merge

import kotlinx.serialization.Serializable

/** Names a record on every device: its kind (e.g. "book") and its `syncId`. */
@Serializable
data class RecordKey(val type: String, val syncId: String)

/**
 * One record as the sync sees it, whatever it is. [data] is its content (opaque here), and it refers
 * to other records by [parents] (by syncId, since the local `id`s differ between devices): a note
 * has its book, a book–collection link has both.
 */
@Serializable
data class SyncRecord(
    val key: RecordKey,
    // Epoch milliseconds of its last change.
    val updatedAt: Long,
    val data: String,
    // What makes two records the same thing even with different syncIds, e.g. a book's file hash.
    val matchKey: String? = null,
    val parents: List<RecordKey> = emptyList(),
)

/** A record that was deleted, so nobody brings it back. */
@Serializable
data class Deletion(val key: RecordKey, val deletedAt: Long)

/** Everything one side knows: its live records and its deletions. The remote one is what goes to the cloud. */
@Serializable
data class SyncSnapshot(
    val records: List<SyncRecord> = emptyList(),
    val deletions: List<Deletion> = emptyList(),
)

/** What to do after merging this device with the cloud. */
data class MergeResult(
    /** The agreed state: what the cloud should hold. */
    val merged: SyncSnapshot,
    /** Records to insert or update on this device (they are new here or newer). */
    val toApply: List<SyncRecord>,
    /** Records this device holds that must be deleted. */
    val toDelete: List<RecordKey>,
    /** Whether [merged] differs from the cloud's snapshot, so it has to be uploaded. */
    val needsUpload: Boolean,
)
