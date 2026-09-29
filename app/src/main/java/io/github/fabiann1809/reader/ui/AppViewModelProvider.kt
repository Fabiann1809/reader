package io.github.fabiann1809.reader.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.ui.addbook.AddBookViewModel
import io.github.fabiann1809.reader.ui.bookdetail.BookDetailViewModel
import io.github.fabiann1809.reader.ui.library.LibraryViewModel
import io.github.fabiann1809.reader.ui.navigation.BookDetailRoute
import io.github.fabiann1809.reader.ui.navigation.NoteEditorRoute
import io.github.fabiann1809.reader.ui.noteeditor.NoteEditorViewModel

/**
 * Creates every ViewModel with its dependencies taken from the AppContainer.
 * Navigation arguments are read here so ViewModels receive plain values and stay easy to test.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            LibraryViewModel(readerApplication().container.bookRepository)
        }
        initializer {
            AddBookViewModel(readerApplication().container.bookRepository)
        }
        initializer {
            BookDetailViewModel(
                bookId = createSavedStateHandle().toRoute<BookDetailRoute>().bookId,
                bookRepository = readerApplication().container.bookRepository,
                noteRepository = readerApplication().container.noteRepository,
            )
        }
        initializer {
            NoteEditorViewModel(
                bookId = createSavedStateHandle().toRoute<NoteEditorRoute>().bookId,
                noteRepository = readerApplication().container.noteRepository,
            )
        }
    }
}

private fun CreationExtras.readerApplication(): ReaderApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReaderApplication
