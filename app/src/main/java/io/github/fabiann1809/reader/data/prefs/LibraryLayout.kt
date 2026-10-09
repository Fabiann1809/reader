package io.github.fabiann1809.reader.data.prefs

/** How the library draws its books (design 6.5). Stored by name, so renaming a constant resets the choice. */
enum class LibraryView {
    SHELVES,
    COLLECTIONS,
    LIST,
}

/** Library view plus how many books fit in each shelf or grid row (the list ignores it). */
data class LibraryLayout(
    val view: LibraryView = LibraryView.SHELVES,
    val booksPerRow: Int = DEFAULT_BOOKS_PER_ROW,
) {
    companion object {
        const val DEFAULT_BOOKS_PER_ROW = 3
        val BooksPerRowRange = 2..4
    }
}
