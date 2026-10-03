package io.github.fabiann1809.reader.data.backup

import androidx.room.withTransaction
import io.github.fabiann1809.reader.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/** Why a backup couldn't be restored (T17.3). */
sealed class BackupError(message: String) : Exception(message) {
    /** Not a backup of this app, or a damaged one. */
    class InvalidFile : BackupError("Not a valid backup")

    /** Made by a newer version of the app, whose format this one doesn't know. */
    class NewerVersion(val version: Int) : BackupError("Backup version $version is newer than this app's")
}

/**
 * Restores a backup made by [BackupExporter] (T17.3), replacing everything the app has: the backup
 * is read and its files unpacked first, so a bad file changes nothing; then the tables are replaced
 * in one transaction, and last the files.
 */
class BackupImporter(
    private val database: AppDatabase,
    private val filesDir: File,
    private val workDir: File,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(input: InputStream): BackupSummary = withContext(Dispatchers.IO) {
        val staging = File(workDir, "restore-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val data = unpack(input, staging)
            if (data.version > BackupData.CURRENT_VERSION) throw BackupError.NewerVersion(data.version)
            replaceTables(data)
            val files = replaceFiles(staging)
            data.summary(files)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Reads `backup.json` and unpacks `files/` into [staging]. */
    private fun unpack(input: InputStream, staging: File): BackupData = try {
        unpackEntries(input, staging)
    } catch (e: ZipException) {
        // A damaged ZIP, or (Android 14+) an entry path that tries to leave its folder.
        throw BackupError.InvalidFile()
    }

    private fun unpackEntries(input: InputStream, staging: File): BackupData {
        var data: BackupData? = null
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                when {
                    entry.name == BackupExporter.DATA_ENTRY -> data = parse(zip.readBytes().decodeToString())
                    entry.name.startsWith(BackupExporter.FILES_DIR) -> {
                        val target = File(staging, entry.name.removePrefix(BackupExporter.FILES_DIR))
                        // A path like "../../x" would write outside the app: such a file isn't a real backup.
                        if (!target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) throw BackupError.InvalidFile()
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
        }
        return data ?: throw BackupError.InvalidFile()
    }

    private fun parse(text: String): BackupData = try {
        json.decodeFromString(BackupData.serializer(), text)
    } catch (e: SerializationException) {
        throw BackupError.InvalidFile()
    } catch (e: IllegalArgumentException) {
        throw BackupError.InvalidFile()
    }

    // Books and collections first: everything else points to them.
    private suspend fun replaceTables(data: BackupData) = database.withTransaction {
        database.bookDao().deleteAll()
        database.collectionDao().deleteAll()
        data.books.forEach { database.bookDao().insert(it) }
        data.collections.forEach { database.collectionDao().insert(it) }
        data.bookCollections.forEach { database.collectionDao().addBook(it) }
        data.notes.forEach { database.noteDao().insert(it) }
        data.bookmarks.forEach { database.bookmarkDao().insert(it) }
        data.highlights.forEach { database.highlightDao().insert(it) }
        data.flashcards.forEach { database.flashcardDao().insert(it) }
        data.sessions.forEach { database.readingSessionDao().insert(it) }
    }

    /** Swaps the app's book, cover and voice folders for the backup's; returns how many files came in. */
    private fun replaceFiles(staging: File): Int {
        APP_FOLDERS.forEach { File(filesDir, it).deleteRecursively() }
        var count = 0
        staging.walkTopDown().filter { it.isFile }.forEach { file ->
            val target = File(filesDir, file.relativeTo(staging).path)
            target.parentFile?.mkdirs()
            file.copyTo(target, overwrite = true)
            count++
        }
        return count
    }

    private companion object {
        // Where BookFiles and VoiceFiles keep the files a backup brings.
        val APP_FOLDERS = listOf("books", "covers", "voice")
    }
}
