package io.github.fabiann1809.reader.data.sync.cloud

import io.github.fabiann1809.reader.data.sync.SyncManifest
import java.io.File

/**
 * [CloudStorage] on Google Drive's hidden app folder (T18.3). The manifest is one file and each
 * book, cover or voice note another, all by name. Drive can't make a write conditional on a version,
 * so a write checks the version just before (not atomic): if two devices still cross, the loser's
 * data is not lost, because it stays on that device and the next sync uploads it again.
 *
 * Every name carries [prefix] (see [CloudNamespace]), so one account only touches its own files.
 */
class DriveCloudStorage(private val drive: DriveApi, private val prefix: String = "") : CloudStorage {

    private val manifestName = prefix + SyncManifest.FILE_NAME

    override suspend fun readManifest(): Result<CloudManifest?> = runCloudCall {
        val file = drive.find(manifestName) ?: return@runCloudCall null
        val content = drive.readText(file.id) ?: return@runCloudCall null
        CloudManifest(content, file.version)
    }

    override suspend fun writeManifest(content: String, expectedVersion: String?): Result<String> = runCloudCall {
        val current = drive.find(manifestName)
        if (current?.version != expectedVersion) throw SyncError.Conflict()
        drive.uploadText(manifestName, content, current).version
    }

    override suspend fun listFiles(): Result<Set<String>> = runCloudCall {
        drive.list().map { it.name }
            .filter { it.startsWith(prefix) && it != manifestName }
            .map { it.removePrefix(prefix) }
            .toSet()
    }

    override suspend fun uploadFile(name: String, file: File): Result<Unit> = runCloudCall {
        requireValid(name)
        drive.uploadFile(prefix + name, file, drive.find(prefix + name))
        Unit
    }

    override suspend fun downloadFile(name: String, into: File): Result<Unit> = runCloudCall {
        val file = drive.find(prefix + name) ?: throw SyncError.Unknown("The file is not in the cloud")
        drive.download(file.id, into)
    }

    override suspend fun deleteFile(name: String): Result<Unit> = runCloudCall {
        drive.find(prefix + name)?.let { drive.delete(it.id) }
        Unit
    }

    override suspend fun adoptLegacyFiles(): Result<Int> = runCloudCall {
        // Without a prefix there is nothing to tell old files from.
        if (prefix.isEmpty()) return@runCloudCall 0
        val all = drive.list()
        val taken = all.map { it.name }.toSet()
        // Files made before libraries were kept per account have no prefix; they become this account's, renamed (never copied or deleted).
        val legacy = all.filter { isLegacy(it.name) && prefix + it.name !in taken }
        legacy.forEach { drive.rename(it.id, prefix + it.name) }
        legacy.size
    }

    private fun isLegacy(name: String) =
        name == SyncManifest.FILE_NAME || LEGACY_PREFIXES.any(name::startsWith)

    private fun requireValid(name: String) {
        require(name != SyncManifest.FILE_NAME && name.matches(FILE_NAME)) { "Invalid cloud file name" }
    }

    private companion object {
        val FILE_NAME = Regex("[A-Za-z0-9._-]{1,120}")
        val LEGACY_PREFIXES = listOf("book-", "cover-", "voice-")
    }
}
