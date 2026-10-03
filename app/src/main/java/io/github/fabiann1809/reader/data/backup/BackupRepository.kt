package io.github.fabiann1809.reader.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

/** Single entry point to backups: writes one to a document the user picked (T17.2), or restores one (T17.3). */
interface BackupRepository {
    /** Writes a backup to the document at [uri]; fails if it can't be opened or written. */
    suspend fun export(uri: String): Result<BackupSummary>

    /**
     * Replaces everything with the backup at [uri]. Fails with a [BackupError] when it isn't a
     * usable backup (then nothing changed), or an IOException when it can't be read.
     */
    suspend fun import(uri: String): Result<BackupSummary>
}

class DefaultBackupRepository(
    private val contentResolver: ContentResolver,
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
) : BackupRepository {

    override suspend fun export(uri: String): Result<BackupSummary> = try {
        val output = contentResolver.openOutputStream(Uri.parse(uri)) ?: throw IOException("Can't open the backup document")
        Result.success(exporter.export(output))
    } catch (e: IOException) {
        Result.failure(e)
    } catch (e: SecurityException) {
        // The document's permission was revoked meanwhile.
        Result.failure(e)
    }

    override suspend fun import(uri: String): Result<BackupSummary> = try {
        val input = contentResolver.openInputStream(Uri.parse(uri)) ?: throw IOException("Can't open the backup document")
        Result.success(importer.import(input))
    } catch (e: BackupError) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(e)
    } catch (e: SecurityException) {
        Result.failure(e)
    }
}
