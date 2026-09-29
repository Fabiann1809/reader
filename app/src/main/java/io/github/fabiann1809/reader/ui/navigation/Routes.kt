package io.github.fabiann1809.reader.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Screens that need arguments use data classes.

@Serializable
data object LibraryRoute

@Serializable
data object AddBookRoute

@Serializable
data class BookDetailRoute(val bookId: Long)

@Serializable
data class NoteEditorRoute(val bookId: Long)

@Serializable
data object SettingsRoute
