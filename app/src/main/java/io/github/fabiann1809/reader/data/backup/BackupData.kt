package io.github.fabiann1809.reader.data.backup

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.session.ReadingSession
import kotlinx.serialization.Serializable

/**
 * Everything a backup holds besides the files (T17.2), as `backup.json` in the ZIP. [version]
 * lets an import (T17.3) tell which format it reads. The API key is never part of it.
 */
@Serializable
data class BackupData(
    val version: Int = CURRENT_VERSION,
    // Epoch milliseconds.
    val exportedAt: Long,
    val books: List<Book> = emptyList(),
    val notes: List<Note> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val bookCollections: List<BookCollectionCrossRef> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val sessions: List<ReadingSession> = emptyList(),
) {
    /** The app files it brings along: books, covers and voice notes, as paths relative to the app's files. */
    val files: List<String>
        get() = (books.flatMap { listOfNotNull(it.filePath, it.coverPath) } + notes.mapNotNull { it.audioPath }).distinct()

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** How much a backup holds, to tell the user (T17.2). */
data class BackupSummary(val books: Int, val notes: Int, val flashcards: Int, val files: Int)

fun BackupData.summary(filesWritten: Int) = BackupSummary(books.size, notes.size, flashcards.size, filesWritten)
