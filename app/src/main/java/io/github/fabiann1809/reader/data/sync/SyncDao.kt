package io.github.fabiann1809.reader.data.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.session.ReadingSession

/**
 * What the sync does to the tables, by syncId (T18.5): the app's own DAOs work by local id, which
 * differs between devices. A delete here leaves its tombstone like any other, through the triggers.
 * An upsert with `id = 0` inserts, and with an existing id updates that row.
 */
@Dao
interface SyncDao {

    @Query("SELECT * FROM books WHERE syncId = :syncId")
    suspend fun book(syncId: String): Book?

    @Query("SELECT * FROM notes WHERE syncId = :syncId")
    suspend fun note(syncId: String): Note?

    @Query("SELECT * FROM collections WHERE syncId = :syncId")
    suspend fun collection(syncId: String): Collection?

    @Query("SELECT * FROM bookmarks WHERE syncId = :syncId")
    suspend fun bookmark(syncId: String): Bookmark?

    @Query("SELECT * FROM highlights WHERE syncId = :syncId")
    suspend fun highlight(syncId: String): Highlight?

    @Query("SELECT * FROM flashcards WHERE syncId = :syncId")
    suspend fun flashcard(syncId: String): Flashcard?

    @Query("SELECT * FROM reading_sessions WHERE syncId = :syncId")
    suspend fun session(syncId: String): ReadingSession?

    @Upsert
    suspend fun upsert(book: Book)

    @Upsert
    suspend fun upsert(note: Note)

    @Upsert
    suspend fun upsert(collection: Collection)

    @Upsert
    suspend fun upsert(bookmark: Bookmark)

    @Upsert
    suspend fun upsert(highlight: Highlight)

    @Upsert
    suspend fun upsert(flashcard: Flashcard)

    @Upsert
    suspend fun upsert(session: ReadingSession)

    @Upsert
    suspend fun upsert(link: BookCollectionCrossRef)

    @Query("DELETE FROM books WHERE syncId = :syncId")
    suspend fun deleteBook(syncId: String)

    @Query("DELETE FROM notes WHERE syncId = :syncId")
    suspend fun deleteNote(syncId: String)

    @Query("DELETE FROM collections WHERE syncId = :syncId")
    suspend fun deleteCollection(syncId: String)

    @Query("DELETE FROM bookmarks WHERE syncId = :syncId")
    suspend fun deleteBookmark(syncId: String)

    @Query("DELETE FROM highlights WHERE syncId = :syncId")
    suspend fun deleteHighlight(syncId: String)

    @Query("DELETE FROM flashcards WHERE syncId = :syncId")
    suspend fun deleteFlashcard(syncId: String)

    @Query("DELETE FROM reading_sessions WHERE syncId = :syncId")
    suspend fun deleteSession(syncId: String)

    @Query("DELETE FROM book_collections WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun deleteLink(bookId: Long, collectionId: Long)

    // The +1 keeps the update trigger from moving updatedAt to now: it would make this device's copy look
    // newer than the cloud's. A millisecond is nothing, and only matters for ties.
    @Query("UPDATE books SET syncId = :newSyncId, updatedAt = updatedAt + 1 WHERE syncId = :oldSyncId")
    suspend fun renameBook(oldSyncId: String, newSyncId: String)

    @Query("UPDATE collections SET syncId = :newSyncId, updatedAt = updatedAt + 1 WHERE syncId = :oldSyncId")
    suspend fun renameCollection(oldSyncId: String, newSyncId: String)

    /** The newest change or deletion in the synced tables (epoch milliseconds), or null when there is none. */
    @Query(
        "SELECT MAX(m) FROM (" +
            "SELECT MAX(updatedAt) AS m FROM books UNION ALL SELECT MAX(updatedAt) FROM notes " +
            "UNION ALL SELECT MAX(updatedAt) FROM collections UNION ALL SELECT MAX(updatedAt) FROM bookmarks " +
            "UNION ALL SELECT MAX(updatedAt) FROM highlights UNION ALL SELECT MAX(updatedAt) FROM flashcards " +
            "UNION ALL SELECT MAX(updatedAt) FROM reading_sessions UNION ALL SELECT MAX(addedAt) FROM book_collections " +
            "UNION ALL SELECT MAX(deletedAt) FROM sync_tombstones)",
    )
    suspend fun lastChangeAt(): Long?

    /** The audio of every voice note of the book with [bookSyncId], to delete with it. */
    @Query("SELECT audioPath FROM notes WHERE audioPath IS NOT NULL AND bookId = (SELECT id FROM books WHERE syncId = :bookSyncId)")
    suspend fun voiceAudioOfBook(bookSyncId: String): List<String>
}
