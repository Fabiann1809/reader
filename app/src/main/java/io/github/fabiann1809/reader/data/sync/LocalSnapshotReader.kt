package io.github.fabiann1809.reader.data.sync

import androidx.room.withTransaction
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.sha256
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.data.sync.merge.Deletion
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import io.github.fabiann1809.reader.data.voice.VoiceFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads the app's tables into a [LocalSnapshot] (T18.5), naming the files the way the cloud does. */
class LocalSnapshotReader(
    private val database: AppDatabase,
    private val bookFiles: BookFiles,
    private val voiceFiles: VoiceFiles,
) {
    suspend fun read(): LocalSnapshot = withContext(Dispatchers.IO) {
        // One transaction so the tables agree with each other (a note never shows without its book).
        val tables = database.withTransaction { Tables.read(database) }
        val files = mutableMapOf<String, File>()
        val bookSyncIds = tables.books.associate { it.id to it.syncId }
        val collectionSyncIds = tables.collections.associate { it.id to it.syncId }
        val records = mutableListOf<SyncRecord>()

        tables.books.forEach { book ->
            val fileName = book.filePath?.let { path -> cloudFile(path, files) { SyncFileNames.book(book.syncId, path) } }
            val coverName = book.coverPath?.let { path -> cloudFile(path, files) { SyncFileNames.cover(bookFiles.resolve(path).sha256()) } }
            records += RecordMapper.record(book, fileName, coverName)
        }
        tables.collections.forEach { records += RecordMapper.record(it) }
        tables.notes.forEach { note ->
            val bookSyncId = bookSyncIds[note.bookId] ?: return@forEach
            val audioName = note.audioPath?.let { path -> cloudFile(path, files) { SyncFileNames.voice(note.syncId) } }
            records += RecordMapper.record(note, bookSyncId, audioName)
        }
        tables.bookmarks.forEach { b -> bookSyncIds[b.bookId]?.let { records += RecordMapper.record(b, it) } }
        tables.highlights.forEach { h -> bookSyncIds[h.bookId]?.let { records += RecordMapper.record(h, it) } }
        tables.flashcards.forEach { f -> bookSyncIds[f.bookId]?.let { records += RecordMapper.record(f, it) } }
        tables.sessions.forEach { s -> bookSyncIds[s.bookId]?.let { records += RecordMapper.record(s, it) } }
        tables.links.forEach { link ->
            val book = bookSyncIds[link.bookId]
            val collection = collectionSyncIds[link.collectionId]
            if (book != null && collection != null) records += RecordMapper.record(link, book, collection)
        }

        val deletions = tables.tombstones.map { Deletion(RecordKey(it.entityType, it.syncId), it.deletedAt) }
        LocalSnapshot(SyncSnapshot(records, deletions), files)
    }

    /** The cloud name of the file at [path], remembering which local file it is; null when the file isn't there. */
    private inline fun cloudFile(path: String, files: MutableMap<String, File>, name: () -> String): String? {
        val file = bookFiles.resolve(path).takeIf { it.exists() } ?: voiceFiles.resolve(path).takeIf { it.exists() } ?: return null
        return name().also { files[it] = file }
    }

    private class Tables(
        val books: List<Book>,
        val collections: List<Collection>,
        val notes: List<Note>,
        val bookmarks: List<Bookmark>,
        val highlights: List<Highlight>,
        val flashcards: List<Flashcard>,
        val sessions: List<ReadingSession>,
        val links: List<BookCollectionCrossRef>,
        val tombstones: List<SyncTombstone>,
    ) {
        companion object {
            suspend fun read(db: AppDatabase) = Tables(
                books = db.bookDao().getAll(),
                collections = db.collectionDao().getAll(),
                notes = db.noteDao().getAll(),
                bookmarks = db.bookmarkDao().getAll(),
                highlights = db.highlightDao().getAll(),
                flashcards = db.flashcardDao().getAll(),
                sessions = db.readingSessionDao().getAll(),
                links = db.collectionDao().getAllLinks(),
                tombstones = db.syncTombstoneDao().getAll(),
            )
        }
    }
}
