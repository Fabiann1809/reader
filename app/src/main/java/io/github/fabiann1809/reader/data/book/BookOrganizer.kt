package io.github.fabiann1809.reader.data.book

import io.github.fabiann1809.reader.data.collection.CollectionRepository

/**
 * Organizing books: favorites, collections, marking them read and deleting them. Shared by the
 * library (one book from its menu, or several selected) and the book detail, so the rules live in one place.
 */
class BookOrganizer(
    private val bookRepository: BookRepository,
    private val collectionRepository: CollectionRepository,
) {
    /** "Mis favoritos" is backed by each book's favorite flag, not by a stored collection. */
    suspend fun setFavorite(bookIds: Collection<Long>, isFavorite: Boolean) =
        bookIds.forEach { id -> updateBook(id) { it.copy(isFavorite = isFavorite) } }

    suspend fun setInCollection(bookId: Long, collectionId: Long, isIncluded: Boolean) {
        if (isIncluded) {
            collectionRepository.addBook(bookId, collectionId)
        } else {
            collectionRepository.removeBook(bookId, collectionId)
        }
    }

    suspend fun addToCollection(bookIds: Collection<Long>, collectionId: Long) =
        bookIds.forEach { collectionRepository.addBook(it, collectionId) }

    /** Creates a collection already holding [bookIds]. Returns its id, or null for a blank name. */
    suspend fun createCollectionWith(bookIds: Collection<Long>, name: String): Long? {
        if (name.isBlank()) return null
        val collectionId = collectionRepository.createCollection(name)
        addToCollection(bookIds, collectionId)
        return collectionId
    }

    suspend fun markAsRead(bookId: Long) = updateBook(bookId) { it.markedAsRead() }

    /** Their notes and collection links go with them (database cascade), and so do their files. */
    suspend fun delete(bookIds: Collection<Long>) =
        bookIds.forEach { id -> bookRepository.getBook(id)?.let { bookRepository.deleteBook(it) } }

    // Reads the stored book rather than what a screen shows, so a stale screen can't overwrite newer data.
    private suspend fun updateBook(bookId: Long, change: (Book) -> Book) {
        val book = bookRepository.getBook(bookId) ?: return
        bookRepository.updateBook(change(book))
    }
}
