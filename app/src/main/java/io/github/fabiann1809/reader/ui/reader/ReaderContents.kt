package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.reader.TocEntry

/** The bookmarks on the page at Readium's [position]; none when the position is unknown. */
fun List<Bookmark>.atPosition(position: Int?): List<Bookmark> =
    if (position == null) emptyList() else filter { it.position == position }

/**
 * The index entry of the open chapter: the first one pointing to the chapter's file [href]. The
 * page doesn't say which section of the file it is in, so the chapter itself is the safe choice.
 * Null when no entry matches.
 */
fun List<TocEntry>.openEntry(href: String?): TocEntry? = if (href == null) null else firstOrNull { it.href == href }
