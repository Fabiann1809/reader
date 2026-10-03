package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.reader.TocEntry

/** A chapter the reader just finished: the discreet "¿Quieres repasar o probarte?" is about it (T15.5). */
data class ChapterEnd(val href: String, val title: String)

/**
 * The chapter finished when the reader went from [fromHref] to [toHref]: only moving on to the very
 * next chapter of the index counts as finishing one (a jump elsewhere, or going back, doesn't).
 */
fun List<TocEntry>.finishedChapter(fromHref: String?, toHref: String?): ChapterEnd? {
    if (fromHref == null || toHref == null || fromHref == toHref) return null
    // Sub-chapters share their file with their parent (with an #anchor), so the index is read by file.
    val chapters = filter { it.file != null }.distinctBy { it.file }
    val from = chapters.indexOfFirst { it.file == fromHref }
    val to = chapters.indexOfFirst { it.file == toHref }
    if (from < 0 || to != from + 1) return null
    return ChapterEnd(fromHref, chapters[from].title.orEmpty())
}

private val TocEntry.file: String? get() = href?.substringBefore('#')
