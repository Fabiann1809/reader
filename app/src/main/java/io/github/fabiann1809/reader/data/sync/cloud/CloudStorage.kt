package io.github.fabiann1809.reader.data.sync.cloud

import java.io.File

/** The manifest as stored: the merged data, and an opaque [version] that changes on every write. */
data class CloudManifest(val content: String, val version: String)

/**
 * The user's private cloud space for the app, whichever service is behind it (Google Drive today,
 * Dropbox later). It holds one manifest with all the records and, next to it, the files (books,
 * covers, voice notes) named by the caller. Everything fails with a [SyncError].
 */
interface CloudStorage {

    /** The manifest, or null when nothing was uploaded yet. */
    suspend fun readManifest(): Result<CloudManifest?>

    /**
     * Replaces the manifest and returns its new version. [expectedVersion] is the version that was
     * read (null when there was none): if the cloud has another one now, fails with [SyncError.Conflict].
     */
    suspend fun writeManifest(content: String, expectedVersion: String?): Result<String>

    /** The names of the files held. */
    suspend fun listFiles(): Result<Set<String>>

    /** Stores [file] under [name], replacing what was there. */
    suspend fun uploadFile(name: String, file: File): Result<Unit>

    /** Writes the file called [name] into [into]; nothing is left behind if it fails. */
    suspend fun downloadFile(name: String, into: File): Result<Unit>

    suspend fun deleteFile(name: String): Result<Unit>

    /**
     * Makes the files this app left in the cloud before each account had a space of its own (T18.9c) part
     * of this account's: they are renamed, never copied or deleted. Returns how many were taken over.
     */
    suspend fun adoptLegacyFiles(): Result<Int>
}
