package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.data.reader.TocEntry

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    /**
     * The book is open in the [ReaderSession]; the navigator for its [format] takes it from there.
     * The page's [chapter], [progression] (0 to 1), [href] and [position] are null until the navigator
     * reports its first page; [positionCount] is the book's total for the page indicator. [contentsVisible] is the index and bookmarks sheet, and
     * [textSettingsVisible] the "Aa" sheet for [readingSettings].
     */
    data class Ready(
        val bookId: Long,
        val format: BookFormat,
        val title: String,
        val chapter: String? = null,
        val progression: Float? = null,
        val href: String? = null,
        val position: Int? = null,
        val positionCount: Int? = null,
        val controlsVisible: Boolean = false,
        val contentsVisible: Boolean = false,
        val tableOfContents: List<TocEntry> = emptyList(),
        val bookmarks: List<Bookmark> = emptyList(),
        val readingSettings: ReadingSettings = ReadingSettings(),
        val textSettingsVisible: Boolean = false,
    ) : ReaderUiState {
        /** True when the open page has a bookmark: the top bar shows it filled. */
        val pageIsBookmarked: Boolean get() = bookmarks.atPosition(position).isNotEmpty()

        /** The index entry of the open chapter, highlighted in the index. */
        val openChapter: TocEntry? get() = tableOfContents.openEntry(href)
    }

    data class CannotOpen(val problem: OpenProblem) : ReaderUiState
}
