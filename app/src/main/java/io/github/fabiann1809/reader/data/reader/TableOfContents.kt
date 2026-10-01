package io.github.fabiann1809.reader.data.reader

import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url

/**
 * One line of a book's table of contents. [level] is 0 for chapters, 1 for their sections and so
 * on; [title] is null when the book gives none. [href] is the chapter's file, to tell which one is open.
 */
data class TocEntry(val index: Int, val title: String?, val level: Int, val href: String)

/** Readium's nested table of contents as flat entries in reading order, each with its depth. */
internal fun flattenToc(links: List<Link>): List<Pair<TocEntry, Link>> {
    val flat = mutableListOf<Pair<TocEntry, Link>>()
    fun add(level: Int, children: List<Link>) {
        children.forEach { link ->
            flat += TocEntry(flat.size, link.title?.takeIf { it.isNotBlank() }, level, link.url().fileHref()) to link
            add(level + 1, link.children)
        }
    }
    add(0, links)
    return flat
}

/** The chapter file of a link or locator, without the "#section" part, to compare them. */
internal fun Url.fileHref(): String = removeFragment().toString()
