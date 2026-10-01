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
import io.github.fabiann1809.reader.ui.explanation.ExplanationLabels
import io.github.fabiann1809.reader.ui.explanation.ExplanationViewModel
import io.github.fabiann1809.reader.ui.extractedtext.ExtractedTextViewModel
import io.github.fabiann1809.reader.ui.library.LibraryViewModel
import io.github.fabiann1809.reader.ui.navigation.BookDetailRoute
import io.github.fabiann1809.reader.ui.navigation.ExplanationRoute
import io.github.fabiann1809.reader.ui.navigation.ExtractedTextRoute
import io.github.fabiann1809.reader.ui.navigation.NoteEditorRoute
import io.github.fabiann1809.reader.ui.noteeditor.NoteEditorViewModel
import io.github.fabiann1809.reader.ui.notes.AllNotesViewModel
import io.github.fabiann1809.reader.ui.onboarding.OnboardingViewModel
import io.github.fabiann1809.reader.ui.settings.SettingsViewModel

/**
 * Creates every ViewModel with its dependencies taken from the AppContainer.
 * Navigation arguments are read here so ViewModels receive plain values and stay easy to test.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            LibraryViewModel(
                bookRepository = readerApplication().container.bookRepository,
                collectionRepository = readerApplication().container.collectionRepository,
                preferences = readerApplication().container.appPreferences,
                bookImporter = readerApplication().container.bookImporter,
            )
        }
        initializer {
            AddBookViewModel(readerApplication().container.bookRepository)
        }
        initializer {
            BookDetailViewModel(
                bookId = createSavedStateHandle().toRoute<BookDetailRoute>().bookId,
                bookRepository = readerApplication().container.bookRepository,
                noteRepository = readerApplication().container.noteRepository,
                collectionRepository = readerApplication().container.collectionRepository,
            )
        }
        initializer {
            val route = createSavedStateHandle().toRoute<NoteEditorRoute>()
            NoteEditorViewModel(
                bookId = route.bookId,
                noteId = route.noteId,
                noteRepository = readerApplication().container.noteRepository,
            )
        }
        initializer {
            ExtractedTextViewModel(
                imageUri = createSavedStateHandle().toRoute<ExtractedTextRoute>().imageUri,
                textRecognizer = readerApplication().container.textRecognizer,
            )
        }
        initializer {
            val route = createSavedStateHandle().toRoute<ExplanationRoute>()
            ExplanationViewModel(
                bookId = route.bookId,
                sourceText = route.sourceText,
                explainText = readerApplication().container.explainText,
                noteRepository = readerApplication().container.noteRepository,
                labels = ExplanationLabels.from(readerApplication()),
            )
        }
        initializer {
            AllNotesViewModel(
                bookRepository = readerApplication().container.bookRepository,
                noteRepository = readerApplication().container.noteRepository,
            )
        }
        initializer {
            AppStartViewModel(readerApplication().container.appPreferences)
        }
        initializer {
            OnboardingViewModel(readerApplication().container.appPreferences)
        }
        initializer {
            SettingsViewModel(
                apiKeyStore = readerApplication().container.apiKeyStore,
                aiProvider = readerApplication().container.aiProvider,
            )
        }
    }
}

private fun CreationExtras.readerApplication(): ReaderApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReaderApplication
