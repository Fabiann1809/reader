package io.github.fabiann1809.reader.data.sync

/** The kind names records and tombstones use, the same on every device. */
object SyncTypes {
    const val BOOK = "book"
    const val NOTE = "note"
    const val COLLECTION = "collection"
    const val BOOKMARK = "bookmark"
    const val HIGHLIGHT = "highlight"
    const val FLASHCARD = "flashcard"
    const val SESSION = "reading_session"

    // A book in a collection: it has no syncId, so it is named "bookSyncId|collectionSyncId".
    const val LINK = "book_collection"

    /** The order records are applied in: a record's parents come before it. */
    val APPLY_ORDER = listOf(COLLECTION, BOOK, NOTE, BOOKMARK, HIGHLIGHT, FLASHCARD, SESSION, LINK)
}
