package io.github.fabiann1809.reader.data.collection

import io.github.fabiann1809.reader.data.book.Book

/**
 * A collection with its books, as the "Colecciones" view shows it. [collection] is null for the
 * default ones (favorites, reading...), which are computed from the books and cannot be edited.
 */
data class CollectionGroup(
    val filter: LibraryFilter,
    val collection: Collection?,
    val smart: SmartCollection?,
    val books: List<Book>,
)

// "Todos" is the whole library, not a group of it.
private val GroupedSmartCollections = listOf(
    SmartCollection.FAVORITES,
    SmartCollection.READING,
    SmartCollection.FINISHED,
    SmartCollection.TO_READ,
)

/**
 * The default collections that have books, then every collection of the user (even empty ones, so
 * books can be added to them). Books go newest first.
 */
fun buildCollectionGroups(
    books: List<Book>,
    collections: List<Collection>,
    links: List<BookCollectionCrossRef>,
): List<CollectionGroup> {
    val newestFirst = books.sortedWith(compareByDescending<Book> { it.createdAt }.thenByDescending { it.id })
    val smartGroups = GroupedSmartCollections.mapNotNull { smart ->
        val members = newestFirst.filter(smart.includes)
        if (members.isEmpty()) null else CollectionGroup(LibraryFilter.Smart(smart), null, smart, members)
    }
    val customGroups = collections.map { collection ->
        val memberIds = links.filter { it.collectionId == collection.id }.mapTo(mutableSetOf()) { it.bookId }
        CollectionGroup(
            filter = LibraryFilter.Custom(collection.id),
            collection = collection,
            smart = null,
            books = newestFirst.filter { it.id in memberIds },
        )
    }
    return smartGroups + customGroups
}
