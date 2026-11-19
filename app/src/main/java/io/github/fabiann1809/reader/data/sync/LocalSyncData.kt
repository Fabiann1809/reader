package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import java.io.File

/** This device's data as the sync sees it, and the files that go with it by their cloud names. */
data class LocalSnapshot(val snapshot: SyncSnapshot, val files: Map<String, File>)

/** Brings a file from the cloud to the device. */
fun interface FileDownloader {
    suspend fun download(cloudName: String, into: File)
}

/**
 * The device's side of the sync: everything the sync reads from and writes to the app's data
 * (T18.5). Behind an interface so the orchestrator is tested without a database.
 */
interface LocalSyncData {

    /** Everything here: live records, deletions, and the local file behind each cloud file name. */
    suspend fun snapshot(): LocalSnapshot

    /** Gives local records the syncId the cloud already uses for the same thing. */
    suspend fun rename(renames: Map<RecordKey, String>)

    /**
     * Writes what the cloud brought: inserts or updates [toApply], deletes [toDelete] (with their files),
     * and downloads the files the records need. All or nothing: if a download fails, nothing changes.
     */
    suspend fun apply(toApply: List<SyncRecord>, toDelete: List<RecordKey>, downloader: FileDownloader)
}
