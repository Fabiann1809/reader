package io.github.fabiann1809.reader.data.book

import java.text.Collator
import java.util.Locale

/** Shelf order. Stored by name in preferences, so renaming a constant resets the user's choice. */
enum class BookSort {
    LAST_READ,
    TITLE,
    AUTHOR,
    DATE_ADDED,
    PROGRESS,
}

/**
 * How the library orders and narrows its books. An empty set means "no filter" for that group;
 * values inside a group are alternatives (EPUB or PDF) and groups combine (EPUB and reading).
 */
data class LibraryArrangement(
    // Newest first was the only order before sorting existed, so it stays the default.
    val sort: BookSort = BookSort.DATE_ADDED,
    val statuses: Set<BookStatus> = emptySet(),
    val formats: Set<BookFormat> = emptySet(),
    val kinds: Set<BookKind> = emptySet(),
) {
    val hasFilters: Boolean
        get() = statuses.isNotEmpty() || formats.isNotEmpty() || kinds.isNotEmpty()

    fun withoutFilters(): LibraryArrangement = LibraryArrangement(sort = sort)
}

fun List<Book>.arrangedBy(arrangement: LibraryArrangement): List<Book> =
    filter { it.passes(arrangement) }.sortedWith(comparatorFor(arrangement.sort))

private fun Book.passes(arrangement: LibraryArrangement): Boolean =
    (arrangement.statuses.isEmpty() || status in arrangement.statuses) &&
        // Physical books have no format, so a format filter leaves them out.
        (arrangement.formats.isEmpty() || format in arrangement.formats) &&
        (arrangement.kinds.isEmpty() || kind in arrangement.kinds)

private fun comparatorFor(sort: BookSort): Comparator<Book> {
    val newestFirst = compareByDescending<Book> { it.createdAt }
    // Collator orders "Ángel" next to "Andrés" instead of after "Z".
    val text = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }
    return when (sort) {
        // Books never opened go last, newest first among themselves.
        BookSort.LAST_READ -> compareByDescending<Book, Long?>(nullsFirst()) { it.lastOpenedAt }.then(newestFirst)
        BookSort.TITLE -> compareBy<Book, String>(text) { it.title }.then(newestFirst)
        BookSort.AUTHOR -> compareBy<Book, String>(text) { it.author }.thenBy(text) { it.title }
        BookSort.DATE_ADDED -> newestFirst
        // Most advanced first; books without a page count go last.
        BookSort.PROGRESS -> compareByDescending<Book, Float?>(nullsFirst()) { it.progress() }.then(newestFirst)
    }
}

private fun Book.progress(): Float? = totalPages?.takeIf { it > 0 }?.let { currentPage.toFloat() / it }
