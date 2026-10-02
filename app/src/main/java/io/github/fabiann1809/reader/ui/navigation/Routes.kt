package io.github.fabiann1809.reader.ui.navigation

import io.github.fabiann1809.reader.ui.noteeditor.NEW_NOTE_ID
import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Screens that need arguments use data classes.

// Shown only on the very first launch.
@Serializable
data object OnboardingRoute

// Top-level destinations, reached from the bottom bar.
@Serializable
data object LibraryRoute

@Serializable
data object ReviewRoute

@Serializable
data object ProgressRoute

@Serializable
data object MoreRoute

@Serializable
data object AddBookRoute

@Serializable
data class BookDetailRoute(val bookId: Long)

// Full-screen reading of a digital book. location (a Locator as JSON, e.g. a note's place) opens it
// there; null opens it where the reader left off.
@Serializable
data class ReaderRoute(val bookId: Long, val location: String? = null)

// sourceText and location come with a note written on a passage in the reader (T11.13).
@Serializable
data class NoteEditorRoute(
    val bookId: Long,
    val noteId: Long = NEW_NOTE_ID,
    val sourceText: String? = null,
    val location: String? = null,
)

@Serializable
data class CaptureRoute(val bookId: Long)

// imageUri is the page image's URI as a string (content:// or file://).
@Serializable
data class ExtractedTextRoute(val bookId: Long, val imageUri: String)

// sourceText is the fragment the user reviewed; it is at most MAX_TEXT_LENGTH characters.
@Serializable
data class ExplanationRoute(val bookId: Long, val sourceText: String)

@Serializable
data object SettingsRoute

@Serializable
data object PrivacyRoute

@Serializable
data object AllNotesRoute

@Serializable
data object BackupRoute

@Serializable
data object AboutRoute
