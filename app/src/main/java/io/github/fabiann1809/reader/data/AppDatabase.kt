package io.github.fabiann1809.reader.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.bookmark.BookmarkDao
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightDao
import io.github.fabiann1809.reader.data.book.BookDao
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionDao
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteDao

@Database(
    entities = [Book::class, Note::class, Collection::class, BookCollectionCrossRef::class, Bookmark::class, Highlight::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        // 1 → 2: new optional Book columns (kind, format, filePath, coverPath, language, lastOpenedAt).
        AutoMigration(from = 1, to = 2),
        // 2 → 3: Book.isFavorite plus the user collections tables.
        AutoMigration(from = 2, to = 3),
        // 3 → 4: Book.readingLocation, where the reader left off.
        AutoMigration(from = 3, to = 4),
        // 4 → 5: the bookmarks table.
        AutoMigration(from = 4, to = 5),
        // 5 → 6: the highlights table.
        AutoMigration(from = 5, to = 6),
        // 6 → 7: Note.location, a note's place in the book.
        AutoMigration(from = 6, to = 7),
    ],
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    abstract fun noteDao(): NoteDao

    abstract fun collectionDao(): CollectionDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun highlightDao(): HighlightDao

    companion object {
        private const val DATABASE_NAME = "reader.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
