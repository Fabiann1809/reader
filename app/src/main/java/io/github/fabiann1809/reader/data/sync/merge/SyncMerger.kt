package io.github.fabiann1809.reader.data.sync.merge

/**
 * Merges what this device holds with what the cloud holds, record by record (T18.2). Pure: no
 * database, no network. Both devices reach the same result, whichever syncs first.
 *
 * Rules, per record:
 * - Both sides have it: the one with the newer `updatedAt` wins.
 * - One side deleted it: the deletion wins unless the record was edited after it (then it comes back).
 *   A tie goes to the deletion.
 * - A record whose parent is gone (deleted, or unknown) goes too: a note can't outlive its book.
 */
object SyncMerger {

    fun merge(local: SyncSnapshot, remote: SyncSnapshot): MergeResult {
        val localRecords = local.records.associateBy { it.key }
        val remoteRecords = remote.records.associateBy { it.key }
        val localDeletions = local.deletions.associate { it.key to it.deletedAt }
        val remoteDeletions = remote.deletions.associate { it.key to it.deletedAt }

        val winners = mutableMapOf<RecordKey, SyncRecord>()
        val deleted = mutableMapOf<RecordKey, Long>()
        val keys = localRecords.keys + remoteRecords.keys + localDeletions.keys + remoteDeletions.keys
        for (key in keys) {
            val record = newest(localRecords[key], remoteRecords[key])
            val deletedAt = listOfNotNull(localDeletions[key], remoteDeletions[key]).maxOrNull()
            when {
                record == null -> deleted[key] = deletedAt!!
                deletedAt != null && deletedAt >= record.updatedAt -> deleted[key] = deletedAt
                else -> winners[key] = record
            }
        }
        dropOrphans(winners, deleted)

        val merged = SyncSnapshot(
            records = winners.values.sortedWith(compareBy({ it.key.type }, { it.key.syncId })),
            deletions = deleted.map { Deletion(it.key, it.value) }.sortedWith(compareBy({ it.key.type }, { it.key.syncId })),
        )
        return MergeResult(
            merged = merged,
            toApply = merged.records.filter { localRecords[it.key]?.updatedAt != it.updatedAt },
            toDelete = localRecords.keys.filter { it !in winners }.sortedWith(compareBy({ it.type }, { it.syncId })),
            needsUpload = merged.records.any { remoteRecords[it.key]?.updatedAt != it.updatedAt } ||
                merged.deletions.any { remoteDeletions[it.key] != it.deletedAt },
        )
    }

    /**
     * Books imported separately on two devices have different syncIds. Returns, for each local
     * record the cloud doesn't know, the cloud's syncId for the same thing (same type and
     * [SyncRecord.matchKey]). The caller renames them before merging: the cloud's id wins.
     */
    fun findRenames(local: SyncSnapshot, remote: SyncSnapshot): Map<RecordKey, String> {
        val knownRemote = remote.records.map { it.key }.toSet() + remote.deletions.map { it.key }
        val localKeys = local.records.map { it.key }.toSet()
        // Remote records no local record already is, by their match key.
        val available = remote.records
            .filter { it.matchKey != null && it.key !in localKeys }
            .groupBy { it.key.type to it.matchKey }
            .mapValues { it.value.map { record -> record.key.syncId }.sorted().toMutableList() }

        val renames = mutableMapOf<RecordKey, String>()
        for (record in local.records.sortedBy { it.key.syncId }) {
            if (record.matchKey == null || record.key in knownRemote) continue
            val syncId = available[record.key.type to record.matchKey]?.removeFirstOrNull() ?: continue
            renames[record.key] = syncId
        }
        return renames
    }

    private fun newest(a: SyncRecord?, b: SyncRecord?): SyncRecord? = when {
        a == null -> b
        b == null -> a
        // On a tie they hold the same change; the order must not matter, so pick by content.
        a.updatedAt == b.updatedAt -> if (a.data <= b.data) a else b
        else -> if (a.updatedAt > b.updatedAt) a else b
    }

    /** Removes the records whose parents didn't survive, and remembers them as deleted with their parent. */
    private fun dropOrphans(winners: MutableMap<RecordKey, SyncRecord>, deleted: MutableMap<RecordKey, Long>) {
        while (true) {
            val orphan = winners.values.firstOrNull { record -> record.parents.any { it !in winners } } ?: return
            val gone = orphan.parents.first { it !in winners }
            winners.remove(orphan.key)
            deleted[orphan.key] = deleted[gone] ?: orphan.updatedAt
        }
    }
}
