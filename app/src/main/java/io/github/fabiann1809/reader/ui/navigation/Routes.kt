package io.github.fabiann1809.reader.ui.navigation

import io.github.fabiann1809.reader.ui.noteeditor.NEW_NOTE_ID
import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Screens that need arguments use data classes.

@Serializable
data object LibraryRoute

@Serializable
data object AddBookRoute

@Serializable
data class BookDetailRoute(val bookId: Long)

@Serializable
data class NoteEditorRoute(val bookId: Long, val noteId: Long = NEW_NOTE_ID)

@Serializable
data object SettingsRoute
