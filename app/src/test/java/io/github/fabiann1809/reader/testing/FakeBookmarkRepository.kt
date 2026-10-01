package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.bookmark.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory BookmarkRepository for ViewModel tests. Mirrors the DAO ordering (reading order). */
class FakeBookmarkRepository(initialBookmarks: List<Bookmark> = emptyList()) : BookmarkRepository {

    private val bookmarks = MutableStateFlow(initialBookmarks)
    private var nextId = (initialBookmarks.maxOfOrNull { it.id } ?: 0) + 1

    val currentBookmarks: List<Bookmark> get() = bookmarks.value

    override fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> = bookmarks.map { list ->
        list.filter { it.bookId == bookId }
            .sortedWith(compareBy<Bookmark> { it.progression }.thenBy { it.createdAt }.thenBy { it.id })
    }

    override suspend fun addBookmark(bookmark: Bookmark): Long {
        val id = nextId++
        bookmarks.update { it + bookmark.copy(id = id) }
        return id
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarks.update { list -> list.filterNot { it.id == bookmark.id } }
    }
}
