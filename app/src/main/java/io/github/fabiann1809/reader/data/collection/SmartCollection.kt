package io.github.fabiann1809.reader.data.collection

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus

/**
 * The default collections of the design. They are computed from each book (favorite flag and status)
 * instead of being stored, so they can never get out of sync. Order is the display order.
 */
enum class SmartCollection(val includes: (Book) -> Boolean) {
    ALL({ true }),
    FAVORITES({ it.isFavorite }),
    READING({ it.status == BookStatus.READING }),
    FINISHED({ it.status == BookStatus.FINISHED }),
    TO_READ({ it.status == BookStatus.TO_READ }),
}

/** What the library is showing: a default collection or one created by the user. */
sealed interface LibraryFilter {
    data class Smart(val collection: SmartCollection) : LibraryFilter

    data class Custom(val collectionId: Long) : LibraryFilter

    /** Stable text form for preferences, e.g. "smart:FAVORITES" or "custom:12". */
    fun encode(): String = when (this) {
        is Smart -> "$SMART:${collection.name}"
        is Custom -> "$CUSTOM:$collectionId"
    }

    companion object {
        val Default: LibraryFilter = Smart(SmartCollection.ALL)

        private const val SMART = "smart"
        private const val CUSTOM = "custom"

        /** Falls back to "Todos" for anything unknown (e.g. a value from an older version). */
        fun decode(value: String?): LibraryFilter {
            val (type, data) = value?.split(":", limit = 2)?.takeIf { it.size == 2 } ?: return Default
            return when (type) {
                SMART -> SmartCollection.entries.firstOrNull { it.name == data }?.let(::Smart)
                CUSTOM -> data.toLongOrNull()?.let(::Custom)
                else -> null
            } ?: Default
        }
    }
}
