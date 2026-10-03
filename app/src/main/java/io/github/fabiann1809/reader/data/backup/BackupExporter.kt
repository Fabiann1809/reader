package io.github.fabiann1809.reader.data.backup

import io.github.fabiann1809.reader.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a backup (T17.2): a ZIP with `backup.json` (every table) and, under `files/`, the books,
 * covers and voice notes they point to. Settings and the API key stay out: the key never leaves
 * the phone's keystore.
 */
class BackupExporter(
    private val database: AppDatabase,
    private val filesDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { encodeDefaults = true }

    /** Writes the backup to [output] (closing it) and says what it holds. */
    suspend fun export(output: OutputStream): BackupSummary = withContext(Dispatchers.IO) {
        val data = collect()
        var filesWritten = 0
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
            zip.closeEntry()
            data.files.forEach { path ->
                val file = File(filesDir, path)
                // A file deleted outside the app is left out; the rest of the backup still holds.
                if (!file.isFile) return@forEach
                zip.putNextEntry(ZipEntry(FILES_DIR + path))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                filesWritten++
            }
        }
        data.summary(filesWritten)
    }

    private suspend fun collect() = BackupData(
        exportedAt = now(),
        books = database.bookDao().getAll(),
        notes = database.noteDao().getAll(),
        collections = database.collectionDao().getAll(),
        bookCollections = database.collectionDao().getAllLinks(),
        bookmarks = database.bookmarkDao().getAll(),
        highlights = database.highlightDao().getAll(),
        flashcards = database.flashcardDao().getAll(),
        sessions = database.readingSessionDao().getAll(),
    )

    companion object {
        const val DATA_ENTRY = "backup.json"
        const val FILES_DIR = "files/"
    }
}
