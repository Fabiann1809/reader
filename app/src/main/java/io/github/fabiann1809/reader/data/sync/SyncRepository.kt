package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.cloud.CloudStorage
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.data.sync.cloud.runCloudCall
import io.github.fabiann1809.reader.data.sync.merge.SyncMerger
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What one sync did, to show the user and to test. */
data class SyncReport(
    // Records written on this device, and deleted from it.
    val applied: Int,
    val deleted: Int,
    val filesUploaded: Int,
    // Whether the cloud's data changed.
    val dataUploaded: Boolean,
)

/** The only entry point to sync (T18.5): brings this device and the cloud to the same state. */
interface SyncRepository {
    /** Fails with a [SyncError]. Syncing while another sync runs waits for it. */
    suspend fun sync(): Result<SyncReport>
}

class DefaultSyncRepository(
    private val local: LocalSyncData,
    private val cloud: CloudStorage,
) : SyncRepository {

    private val lock = Mutex()

    override suspend fun sync(): Result<SyncReport> = lock.withLock {
        repeat(MAX_ATTEMPTS) {
            val result = runCloudCall { attempt() }
            // Another device wrote the manifest while this one worked: merge again with what it left.
            if (result.exceptionOrNull() !is SyncError.Conflict) return@withLock result
        }
        Result.failure(SyncError.Conflict())
    }

    private suspend fun attempt(): SyncReport {
        val manifest = cloud.readManifest().getOrThrow()
        val remote = manifest?.let { SyncManifest.decode(it.content) } ?: SyncSnapshot()

        var snapshot = local.snapshot()
        val renames = SyncMerger.findRenames(snapshot.snapshot, remote)
        if (renames.isNotEmpty()) {
            local.rename(renames)
            snapshot = local.snapshot()
        }

        val merge = SyncMerger.merge(snapshot.snapshot, remote)
        local.apply(merge.toApply, merge.toDelete) { name, into -> cloud.downloadFile(name, into).getOrThrow() }

        // The files go up before the manifest that names them, so nobody reads a record whose file isn't there yet.
        val cloudFiles = cloud.listFiles().getOrThrow()
        val needed = merge.merged.records.flatMap(SyncFileNames::of).toSet()
        val toUpload = needed.filter { it !in cloudFiles && it in snapshot.files }
        toUpload.forEach { cloud.uploadFile(it, snapshot.files.getValue(it)).getOrThrow() }

        if (merge.needsUpload) {
            cloud.writeManifest(SyncManifest.encode(merge.merged), manifest?.version).getOrThrow()
        }
        deleteOrphans(remote, needed)
        return SyncReport(merge.toApply.size, merge.toDelete.size, toUpload.size, merge.needsUpload)
    }

    /**
     * Removes the files the cloud's data used to name and nothing names now (a deleted book, a changed cover).
     * Only those: a file nobody named yet may be one another device is still uploading.
     */
    private suspend fun deleteOrphans(previous: SyncSnapshot, needed: Set<String>) {
        previous.records.flatMap(SyncFileNames::of)
            .filter { it !in needed && SyncFileNames.isManaged(it) }
            .toSet()
            // A failed delete only leaves a file behind; the next sync tries again.
            .forEach { cloud.deleteFile(it) }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
