package io.github.fabiann1809.reader.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.fabiann1809.reader.data.sync.SyncTriggers

/**
 * Manual schema migrations, in order. Simple additions use `AutoMigration` in [AppDatabase] instead
 * (1 → 2 is automatic). The database already holds the user's books and notes, so a new version
 * must never be destructive.
 *
 * To change the schema:
 * 1. Bump `version` in [AppDatabase] and build: Room exports the new schema to `app/schemas`.
 * 2. Add a `Migration(old, new)` here (or an `AutoMigration` in [AppDatabase] for simple additions).
 * 3. Extend `MigrationTest` with data that exercises the change.
 */
// 12 → 13: Collection.colorIndex. Collections made before it had the color their id picked, so they keep it.
internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE collections ADD COLUMN colorIndex INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE collections SET colorIndex = id % 4")
    }
}

// 13 → 14: sync bookkeeping (T18.1). Every record gets a syncId and an updatedAt, deletions are remembered
// in sync_tombstones, and triggers keep both up to date. Existing records get a new random syncId and
// their last known activity as updatedAt.
internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS sync_tombstones (entityType TEXT NOT NULL, syncId TEXT NOT NULL, " +
                "deletedAt INTEGER NOT NULL, PRIMARY KEY(entityType, syncId))",
        )
        // Where each table's last known activity is.
        mapOf(
            "books" to "MAX(createdAt, COALESCE(lastOpenedAt, 0), COALESCE(finishedAt, 0))",
            "notes" to "createdAt",
            "collections" to "createdAt",
            "bookmarks" to "createdAt",
            "highlights" to "createdAt",
            "flashcards" to "MAX(createdAt, nextReviewAt)",
            "reading_sessions" to "endedAt",
        ).forEach { (table, activity) -> addSyncColumns(db, table, activity) }
        SyncTriggers.install(db)
    }

    private fun addSyncColumns(db: SupportSQLiteDatabase, table: String, activity: String) {
        db.execSQL("ALTER TABLE $table ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE $table SET syncId = $RANDOM_UUID, updatedAt = $activity")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_syncId ON $table (syncId)")
    }
}

// A random version 4 UUID in SQL, so each existing row gets its own value.
private const val RANDOM_UUID =
    "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || " +
        "substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"

internal val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_12_13, MIGRATION_13_14)
