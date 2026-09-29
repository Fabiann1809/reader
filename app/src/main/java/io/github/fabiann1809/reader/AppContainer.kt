package io.github.fabiann1809.reader

import android.content.Context
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.DefaultBookRepository
import io.github.fabiann1809.reader.data.note.DefaultNoteRepository
import io.github.fabiann1809.reader.data.note.NoteRepository

/**
 * Manual dependency injection: builds the app-wide singletons once.
 * Everything is lazy so nothing is created (e.g. the database) until first use.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    val bookRepository: BookRepository by lazy { DefaultBookRepository(database.bookDao()) }

    val noteRepository: NoteRepository by lazy { DefaultNoteRepository(database.noteDao()) }
}
