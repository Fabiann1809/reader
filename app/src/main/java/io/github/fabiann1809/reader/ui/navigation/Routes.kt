package io.github.fabiann1809.reader.ui.navigation

import io.github.fabiann1809.reader.ui.noteeditor.NEW_NOTE_ID
import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Screens that need arguments use data classes.

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

@Serializable
data class NoteEditorRoute(val bookId: Long, val noteId: Long = NEW_NOTE_ID)

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
