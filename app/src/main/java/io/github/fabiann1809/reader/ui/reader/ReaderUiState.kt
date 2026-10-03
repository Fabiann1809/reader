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
     * [textSettingsVisible] the "Aa" sheet for [readingSettings]. [selection] is the text selected on
     * the page, which shows the selection capsule, and [explaining] the text in the explainer sheet.
     * [zonePicking] is set while a zone of a PDF page is being marked to explain it, and
     * [recordingVoice] while the "Grabar" sheet is open. [menuVisible] is the ⋮ sheet, and
     * [chapterQuiz] a quiz about the open chapter that is ready to start (T15.3).
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
        val selection: TextSelection? = null,
        val explaining: String? = null,
        val zonePicking: ZonePicking? = null,
        val recordingVoice: Boolean = false,
        val menuVisible: Boolean = false,
        val chapterQuiz: ChapterQuiz? = null,
        val chapterUnreadable: Boolean = false,
    ) : ReaderUiState {
        /** True when the open page has a bookmark: the top bar shows it filled. */
        val pageIsBookmarked: Boolean get() = bookmarks.atPosition(position).isNotEmpty()

        /** The index entry of the open chapter, highlighted in the index. */
        val openChapter: TocEntry? get() = tableOfContents.openEntry(href)
    }

    data class CannotOpen(val problem: OpenProblem) : ReaderUiState
}

/** Marking a zone of a PDF page to explain it (T11.14): drawing it, reading it, or nothing found. */
enum class ZonePicking { MARKING, READING, NO_TEXT }
