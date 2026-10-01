package io.github.fabiann1809.reader.data.bookmark

import kotlinx.coroutines.flow.Flow

/** Single entry point to bookmarks. ViewModels depend on this interface so tests can use fakes. */
interface BookmarkRepository {
    /** The bookmarks of [bookId], in reading order. */
    fun observeBookmarks(bookId: Long): Flow<List<Bookmark>>

    /** Returns the id of the new bookmark. */
    suspend fun addBookmark(bookmark: Bookmark): Long

    suspend fun deleteBookmark(bookmark: Bookmark)
}

class DefaultBookmarkRepository(private val bookmarkDao: BookmarkDao) : BookmarkRepository {
    override fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> = bookmarkDao.observeByBook(bookId)

    override suspend fun addBookmark(bookmark: Bookmark): Long = bookmarkDao.insert(bookmark)

    override suspend fun deleteBookmark(bookmark: Bookmark) = bookmarkDao.delete(bookmark)
}
