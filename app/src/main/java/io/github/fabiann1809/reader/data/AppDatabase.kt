package io.github.fabiann1809.reader.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookDao
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionDao
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteDao

@Database(
    entities = [Book::class, Note::class, Collection::class, BookCollectionCrossRef::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // 1 → 2: new optional Book columns (kind, format, filePath, coverPath, language, lastOpenedAt).
        AutoMigration(from = 1, to = 2),
        // 2 → 3: Book.isFavorite plus the user collections tables.
        AutoMigration(from = 2, to = 3),
        // 3 → 4: Book.readingLocation, where the reader left off.
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    abstract fun noteDao(): NoteDao

    abstract fun collectionDao(): CollectionDao

    companion object {
        private const val DATABASE_NAME = "reader.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
