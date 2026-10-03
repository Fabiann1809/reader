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

/** A review session of today's cards (T14.4); only [bookId]'s when set ("Repasa este libro", T16.4). */
@Serializable
data class ReviewSessionRoute(val bookId: Long = ALL_BOOKS) {
    companion object {
        const val ALL_BOOKS = 0L
    }
}

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

/**
 * "Nueva ficha" (T14.2) for [bookId]. A card from a note, explanation or selection brings its
 * [source] text, [page] (0 when unknown) and the note's [tag] (a NoteTag name).
 */
@Serializable
data class FlashcardEditorRoute(
    val bookId: Long,
    val source: String? = null,
    val page: Int = 0,
    val tag: String? = null,
)

/** "Ahora tú" (T15.1): writing in one's own words what [sourceText] means. */
@Serializable
data class InterpretationRoute(val bookId: Long, val sourceText: String)

/**
 * A quiz of [count] questions about [source] (T15.3); [title] heads it (a chapter, "Repaso"...). The
 * cards made from its mistakes (T15.4) go to [bookId].
 */
@Serializable
data class QuizRoute(val bookId: Long, val source: String, val count: Int, val title: String)

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
