package io.github.fabiann1809.reader.data

import androidx.room.migration.Migration

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
internal val ALL_MIGRATIONS: Array<Migration> = arrayOf()
