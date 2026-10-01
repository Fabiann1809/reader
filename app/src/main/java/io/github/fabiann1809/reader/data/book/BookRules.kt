package io.github.fabiann1809.reader.data.book

import java.util.concurrent.TimeUnit

/** How long a book that was never started shows the "Nuevo" badge. */
private val NEW_BOOK_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7)

/** Added in the last week and not started yet. */
fun Book.isNew(now: Long = System.currentTimeMillis()): Boolean =
    status == BookStatus.TO_READ && currentPage == 0 && lastOpenedAt == null && now - createdAt < NEW_BOOK_WINDOW_MILLIS

/** Finished, with the progress moved to the last page when the page count is known. */
fun Book.markedAsRead(): Book = copy(status = BookStatus.FINISHED, currentPage = totalPages ?: currentPage)
