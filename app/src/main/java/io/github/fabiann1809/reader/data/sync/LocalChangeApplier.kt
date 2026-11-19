package io.github.fabiann1809.reader.data.sync

import androidx.room.withTransaction
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.sha256
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.data.voice.VoiceFiles
import java.io.File

/**
 * Writes into the app's tables what the cloud brought (T18.5): first downloads the files, then
 * changes the rows in one transaction, and only after that removes the local files nothing uses any more.
 */
class LocalChangeApplier(
    private val database: AppDatabase,
    private val bookFiles: BookFiles,
    private val voiceFiles: VoiceFiles,
) {
    private val dao = database.syncDao()

    /** Where a record's files ended up on this device (relative paths), after [prepareFiles]. */
    private class LocalFiles(val file: String? = null, val cover: String? = null)

    suspend fun apply(toApply: List<SyncRecord>, toDelete: List<RecordKey>, downloader: FileDownloader) {
        val downloaded = mutableListOf<File>()
        try {
            val localFiles = toApply.associate { it.key to prepareFiles(it, downloader, downloaded) }
            val unused = database.withTransaction {
                val unused = mutableListOf<String>()
                toDelete.forEach { delete(it, unused) }
                toApply.groupBy { it.key.type }.let { byType ->
                    SyncTypes.APPLY_ORDER.forEach { type ->
                        byType[type].orEmpty().forEach { upsert(it, localFiles.getValue(it.key), unused) }
                    }
                }
                unused
            }
            unused.forEach(bookFiles::delete)
        } catch (e: Throwable) {
            // Nothing was written, so the files that came for it are no use.
            downloaded.forEach(File::delete)
            throw e
        }
    }

    private suspend fun prepareFiles(record: SyncRecord, downloader: FileDownloader, downloaded: MutableList<File>): LocalFiles {
        suspend fun fetch(cloudName: String, into: File): String {
            downloaded += into
            downloader.download(cloudName, into)
            return bookFiles.relativePath(into)
        }
        return when (record.key.type) {
            SyncTypes.BOOK -> {
                val existing = dao.book(record.key.syncId)
                val fileName = RecordMapper.cloudName(record, "filePath")
                val coverName = RecordMapper.cloudName(record, "coverPath")
                LocalFiles(
                    file = when {
                        fileName == null -> null
                        // A book's file never changes, so having it is enough.
                        existing?.filePath?.let { bookFiles.resolve(it).exists() } == true -> existing.filePath
                        else -> fetch(fileName, bookFiles.newFile(SyncFileNames.extensionOf(fileName, "bin")))
                    },
                    cover = when {
                        coverName == null -> null
                        existing?.coverPath != null && hasCover(existing.coverPath, coverName) -> existing.coverPath
                        else -> fetch(coverName, bookFiles.newCoverFile())
                    },
                )
            }
            SyncTypes.NOTE -> {
                val audioName = RecordMapper.cloudName(record, "audioPath")
                val existing = dao.note(record.key.syncId)
                LocalFiles(
                    file = when {
                        audioName == null -> null
                        existing?.audioPath?.let { voiceFiles.resolve(it).exists() } == true -> existing.audioPath
                        else -> fetch(audioName, voiceFiles.newSyncedRecording())
                    },
                )
            }
            else -> LocalFiles()
        }
    }

    private fun hasCover(path: String, cloudName: String): Boolean =
        bookFiles.resolve(path).let { it.exists() && SyncFileNames.cover(it.sha256()) == cloudName }

    /** Deletes the record for [key]; adds the files that went with it to [unused]. */
    private suspend fun delete(key: RecordKey, unused: MutableList<String>) {
        val syncId = key.syncId
        when (key.type) {
            SyncTypes.BOOK -> {
                dao.book(syncId)?.let { unused += listOfNotNull(it.filePath, it.coverPath) }
                unused += dao.voiceAudioOfBook(syncId)
                dao.deleteBook(syncId)
            }
            SyncTypes.NOTE -> {
                dao.note(syncId)?.audioPath?.let(unused::add)
                dao.deleteNote(syncId)
            }
            SyncTypes.COLLECTION -> dao.deleteCollection(syncId)
            SyncTypes.BOOKMARK -> dao.deleteBookmark(syncId)
            SyncTypes.HIGHLIGHT -> dao.deleteHighlight(syncId)
            SyncTypes.FLASHCARD -> dao.deleteFlashcard(syncId)
            SyncTypes.SESSION -> dao.deleteSession(syncId)
            SyncTypes.LINK -> {
                val (bookSyncId, collectionSyncId) = RecordMapper.splitLinkId(syncId)
                val bookId = dao.book(bookSyncId)?.id
                val collectionId = dao.collection(collectionSyncId)?.id
                if (bookId != null && collectionId != null) dao.deleteLink(bookId, collectionId)
            }
        }
    }

    private suspend fun upsert(record: SyncRecord, files: LocalFiles, unused: MutableList<String>) {
        val syncId = record.key.syncId
        // A child whose book isn't here can't be saved; the merge makes sure that doesn't happen.
        val bookId = record.parents.firstOrNull { it.type == SyncTypes.BOOK }?.let { dao.book(it.syncId)?.id }
        when (record.key.type) {
            SyncTypes.BOOK -> {
                val existing = dao.book(syncId)
                dao.upsert(RecordMapper.book(record, existing?.id ?: 0, files.file, files.cover))
                existing?.coverPath?.takeIf { it != files.cover }?.let(unused::add)
            }
            SyncTypes.COLLECTION -> dao.upsert(RecordMapper.collection(record, dao.collection(syncId)?.id ?: 0))
            SyncTypes.NOTE -> if (bookId != null) {
                val existing = dao.note(syncId)
                dao.upsert(RecordMapper.note(record, existing?.id ?: 0, bookId, files.file))
                existing?.audioPath?.takeIf { it != files.file }?.let(unused::add)
            }
            SyncTypes.BOOKMARK -> if (bookId != null) dao.upsert(RecordMapper.bookmark(record, dao.bookmark(syncId)?.id ?: 0, bookId))
            SyncTypes.HIGHLIGHT -> if (bookId != null) dao.upsert(RecordMapper.highlight(record, dao.highlight(syncId)?.id ?: 0, bookId))
            SyncTypes.FLASHCARD -> if (bookId != null) dao.upsert(RecordMapper.flashcard(record, dao.flashcard(syncId)?.id ?: 0, bookId))
            SyncTypes.SESSION -> if (bookId != null) dao.upsert(RecordMapper.session(record, dao.session(syncId)?.id ?: 0, bookId))
            SyncTypes.LINK -> {
                val (bookSyncId, collectionSyncId) = RecordMapper.splitLinkId(syncId)
                val linkedBook = dao.book(bookSyncId)?.id
                val linkedCollection = dao.collection(collectionSyncId)?.id
                if (linkedBook != null && linkedCollection != null) dao.upsert(RecordMapper.link(record, linkedBook, linkedCollection))
            }
        }
    }
}
