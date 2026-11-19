package io.github.fabiann1809.reader.data.sync

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SQLite triggers that keep the sync bookkeeping on their own, so no DAO or screen has to:
 * - after an UPDATE that leaves `updatedAt` as it was (or older), it is set to now;
 * - after a DELETE (also a cascaded one) a row goes into `sync_tombstones`;
 * - after an INSERT of a record that had been deleted (an undo), the tombstone is dropped and
 *   `updatedAt` moves to now, so the restore wins over the old deletion on the other devices.
 *
 * A write that brings a newer `updatedAt` (applying what another device sent) is left as it is.
 * Room doesn't know about triggers, so they are installed when the database is created
 * ([SyncTriggersCallback]) and by the migration that introduced them.
 */
object SyncTriggers {

    /** Table name to the type name its tombstones use. */
    private val TABLES = mapOf(
        "books" to "book",
        "notes" to "note",
        "collections" to "collection",
        "bookmarks" to "bookmark",
        "highlights" to "highlight",
        "flashcards" to "flashcard",
        "reading_sessions" to "reading_session",
    )

    const val LINK_TYPE = "book_collection"

    // Epoch milliseconds in SQL (julianday 2440587.5 is the Unix epoch).
    private const val NOW = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"

    fun install(db: SupportSQLiteDatabase) {
        TABLES.forEach { (table, type) -> tableTriggers(table, type).forEach(db::execSQL) }
        linkTriggers().forEach(db::execSQL)
    }

    private fun tableTriggers(table: String, type: String) = listOf(
        """
        CREATE TRIGGER IF NOT EXISTS sync_${table}_update AFTER UPDATE ON $table
        WHEN NEW.updatedAt <= OLD.updatedAt
        BEGIN
            UPDATE $table SET updatedAt = MAX($NOW, OLD.updatedAt + 1) WHERE id = NEW.id;
        END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS sync_${table}_delete AFTER DELETE ON $table
        BEGIN
            INSERT OR REPLACE INTO sync_tombstones (entityType, syncId, deletedAt) VALUES ('$type', OLD.syncId, $NOW);
        END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS sync_${table}_insert AFTER INSERT ON $table
        WHEN EXISTS (SELECT 1 FROM sync_tombstones WHERE entityType = '$type' AND syncId = NEW.syncId)
        BEGIN
            DELETE FROM sync_tombstones WHERE entityType = '$type' AND syncId = NEW.syncId;
            UPDATE $table SET updatedAt = MAX($NOW, NEW.updatedAt) WHERE id = NEW.id;
        END
        """,
    ).map { it.trimIndent() }

    // A link has no syncId: it is told apart by its two books' and collections' syncIds. When a book or
    // collection is deleted its links go with it, and then there is nothing to name them by (and the
    // other device deletes them with the book or collection anyway), so no tombstone is written.
    private fun linkTriggers() = listOf(
        """
        CREATE TRIGGER IF NOT EXISTS sync_book_collections_delete AFTER DELETE ON book_collections
        BEGIN
            INSERT OR REPLACE INTO sync_tombstones (entityType, syncId, deletedAt)
            SELECT '$LINK_TYPE', b.syncId || '|' || c.syncId, $NOW
            FROM books b, collections c WHERE b.id = OLD.bookId AND c.id = OLD.collectionId;
        END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS sync_book_collections_insert AFTER INSERT ON book_collections
        WHEN EXISTS (
            SELECT 1 FROM sync_tombstones t, books b, collections c
            WHERE t.entityType = '$LINK_TYPE' AND t.syncId = b.syncId || '|' || c.syncId
            AND b.id = NEW.bookId AND c.id = NEW.collectionId
        )
        BEGIN
            DELETE FROM sync_tombstones WHERE entityType = '$LINK_TYPE' AND syncId =
                (SELECT b.syncId || '|' || c.syncId FROM books b, collections c WHERE b.id = NEW.bookId AND c.id = NEW.collectionId);
            UPDATE book_collections SET addedAt = MAX($NOW, NEW.addedAt)
            WHERE bookId = NEW.bookId AND collectionId = NEW.collectionId;
        END
        """,
    ).map { it.trimIndent() }
}

/** Installs [SyncTriggers] when Room creates a database from scratch (migrations install them on upgrades). */
object SyncTriggersCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
}
