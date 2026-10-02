package io.github.fabiann1809.reader.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.ui.addbook.AddBookViewModel
import io.github.fabiann1809.reader.ui.bookdetail.BookDetailViewModel
import io.github.fabiann1809.reader.ui.capture.PhotoCropViewModel
import io.github.fabiann1809.reader.ui.explanation.ExplanationLabels
import io.github.fabiann1809.reader.ui.explanation.ExplanationViewModel
import io.github.fabiann1809.reader.ui.extractedtext.ExtractedTextViewModel
import io.github.fabiann1809.reader.ui.flashcardeditor.FlashcardEditorViewModel
import io.github.fabiann1809.reader.ui.library.LibraryViewModel
import io.github.fabiann1809.reader.ui.library.WatchedFolderViewModel
import io.github.fabiann1809.reader.ui.navigation.BookDetailRoute
import io.github.fabiann1809.reader.ui.navigation.ExplanationRoute
import io.github.fabiann1809.reader.ui.navigation.ExtractedTextRoute
import io.github.fabiann1809.reader.ui.navigation.FlashcardEditorRoute
import io.github.fabiann1809.reader.ui.navigation.NoteEditorRoute
import io.github.fabiann1809.reader.ui.navigation.ReaderRoute
import io.github.fabiann1809.reader.ui.noteeditor.NoteEditorViewModel
import io.github.fabiann1809.reader.ui.notes.AllNotesViewModel
import io.github.fabiann1809.reader.ui.onboarding.OnboardingViewModel
import io.github.fabiann1809.reader.ui.reader.ReaderViewModel
import io.github.fabiann1809.reader.ui.review.PendingReviewsViewModel
import io.github.fabiann1809.reader.ui.review.ReviewViewModel
import io.github.fabiann1809.reader.ui.settings.SettingsViewModel
import io.github.fabiann1809.reader.ui.voice.VoiceNotePlayerViewModel
import io.github.fabiann1809.reader.ui.voice.VoiceRecordingViewModel

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
                importQueue = readerApplication().container.importQueue,
                organizer = readerApplication().container.bookOrganizer,
            )
        }
        initializer {
            WatchedFolderViewModel(
                manager = readerApplication().container.watchedFolderManager,
                fallbackName = readerApplication().getString(R.string.watched_folder_default_name),
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
                highlightRepository = readerApplication().container.highlightRepository,
                organizer = readerApplication().container.bookOrganizer,
            )
        }
        initializer {
            val route = createSavedStateHandle().toRoute<ReaderRoute>()
            ReaderViewModel(
                bookId = route.bookId,
                startAt = route.location,
                bookRepository = readerApplication().container.bookRepository,
                bookmarkRepository = readerApplication().container.bookmarkRepository,
                highlightRepository = readerApplication().container.highlightRepository,
                readingPreferences = readerApplication().container.readingPreferences,
                textRecognizer = readerApplication().container.textRecognizer,
                session = readerApplication().container.readerSession,
            )
        }
        initializer {
            val route = createSavedStateHandle().toRoute<NoteEditorRoute>()
            NoteEditorViewModel(
                bookId = route.bookId,
                noteId = route.noteId,
                noteRepository = readerApplication().container.noteRepository,
                sourceText = route.sourceText,
                location = route.location,
            )
        }
        initializer {
            val route = createSavedStateHandle().toRoute<FlashcardEditorRoute>()
            FlashcardEditorViewModel(
                bookId = route.bookId,
                flashcardRepository = readerApplication().container.flashcardRepository,
                makeFlashcard = readerApplication().container.makeFlashcard,
                source = route.source,
                page = route.page.takeIf { it > 0 },
                tag = route.tag?.let { name -> NoteTag.entries.find { it.name == name } },
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
            VoiceNotePlayerViewModel(readerApplication().container.newVoicePlayer())
        }
        initializer {
            ReviewViewModel(
                flashcardRepository = readerApplication().container.flashcardRepository,
                bookRepository = readerApplication().container.bookRepository,
            )
        }
        initializer {
            PendingReviewsViewModel(readerApplication().container.flashcardRepository)
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
        initializer {
            VoiceRecordingViewModel(
                recorder = readerApplication().container.voiceRecorder,
                player = readerApplication().container.newVoicePlayer(),
                transcribe = readerApplication().container.transcribeAudio,
                noteRepository = readerApplication().container.noteRepository,
            )
        }
    }

    /** The crop of a photo that was just taken (T12.5), one per photo. */
    fun photoCrop(imageUri: String) = viewModelFactory {
        initializer { PhotoCropViewModel(imageUri, readerApplication().container.textRecognizer) }
    }

    /** The reader's explainer sheet (T11.11): its text comes from the page's selection, not from a route. */
    fun explanationInReader(bookId: Long, sourceText: String) = viewModelFactory {
        initializer {
            ExplanationViewModel(
                bookId = bookId,
                sourceText = sourceText,
                explainText = readerApplication().container.explainText,
                noteRepository = readerApplication().container.noteRepository,
                labels = ExplanationLabels.from(readerApplication()),
            )
        }
    }
}

private fun CreationExtras.readerApplication(): ReaderApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReaderApplication
