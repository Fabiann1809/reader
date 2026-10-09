package io.github.fabiann1809.reader.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

internal val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_12_13)
