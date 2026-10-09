package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.data.prefs.LibraryLayout

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val filter: LibraryFilter = LibraryFilter.Default,
    /** The selected user collection, when [filter] is a custom one. */
    val currentCollection: Collection? = null,
    /** User collections, for the collection picker. */
    val collections: List<Collection> = emptyList(),
    /** True when the user has no books at all (not just none in this collection). */
    val libraryIsEmpty: Boolean = false,
    /** Search text; [books] only holds the matches when it is not blank. */
    val query: String = "",
    /** Shelf order and filters already applied to [books]. */
    val arrangement: LibraryArrangement = LibraryArrangement(),
    val layout: LibraryLayout = LibraryLayout(),
    /** Books checked in selection mode; empty when not selecting. Only ids of shown books. */
    val selectedIds: Set<Long> = emptySet(),
    /** What "Continuar" opens, from the whole library whatever the shelf shows. Null if nothing was read yet. */
    val bookToContinue: Book? = null,
    /** Books in each default collection, from the whole library: the numbers on the header chips. */
    val smartCounts: Map<SmartCollection, Int> = emptyMap(),
) {
    val isSelecting: Boolean
        get() = selectedIds.isNotEmpty()
}

/** The book whose "Colección" sheet is open, with the user collections that already hold it. */
data class BookCollections(val book: Book, val collectionIds: Set<Long>)
