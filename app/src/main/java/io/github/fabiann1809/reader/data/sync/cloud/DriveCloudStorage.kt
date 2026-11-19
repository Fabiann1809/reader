package io.github.fabiann1809.reader.data.sync.cloud

import io.github.fabiann1809.reader.data.sync.SyncManifest
import java.io.File

/**
 * [CloudStorage] on Google Drive's hidden app folder (T18.3). The manifest is one file and each
 * book, cover or voice note another, all by name. Drive can't make a write conditional on a version,
 * so a write checks the version just before (not atomic): if two devices still cross, the loser's
 * data is not lost, because it stays on that device and the next sync uploads it again.
 */
class DriveCloudStorage(private val drive: DriveApi) : CloudStorage {

    override suspend fun readManifest(): Result<CloudManifest?> = runCloudCall {
        val file = drive.find(SyncManifest.FILE_NAME) ?: return@runCloudCall null
        val content = drive.readText(file.id) ?: return@runCloudCall null
        CloudManifest(content, file.version)
    }

    override suspend fun writeManifest(content: String, expectedVersion: String?): Result<String> = runCloudCall {
        val current = drive.find(SyncManifest.FILE_NAME)
        if (current?.version != expectedVersion) throw SyncError.Conflict()
        drive.uploadText(SyncManifest.FILE_NAME, content, current).version
    }

    override suspend fun listFiles(): Result<Set<String>> = runCloudCall {
        drive.list().map { it.name }.filter { it != SyncManifest.FILE_NAME }.toSet()
    }

    override suspend fun uploadFile(name: String, file: File): Result<Unit> = runCloudCall {
        requireValid(name)
        drive.uploadFile(name, file, drive.find(name))
        Unit
    }

    override suspend fun downloadFile(name: String, into: File): Result<Unit> = runCloudCall {
        val file = drive.find(name) ?: throw SyncError.Unknown("The file is not in the cloud")
        drive.download(file.id, into)
    }

    override suspend fun deleteFile(name: String): Result<Unit> = runCloudCall {
        drive.find(name)?.let { drive.delete(it.id) }
        Unit
    }

    private fun requireValid(name: String) {
        require(name != SyncManifest.FILE_NAME && name.matches(FILE_NAME)) { "Invalid cloud file name" }
    }

    private companion object {
        val FILE_NAME = Regex("[A-Za-z0-9._-]{1,120}")
    }
}
