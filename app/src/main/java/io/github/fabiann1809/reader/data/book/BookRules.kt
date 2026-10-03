package io.github.fabiann1809.reader.data.book

import java.util.concurrent.TimeUnit

/** How long a book that was never started shows the "Nuevo" badge. */
private val NEW_BOOK_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7)

/** Added in the last week and not started yet. */
fun Book.isNew(now: Long = System.currentTimeMillis()): Boolean =
    status == BookStatus.TO_READ && currentPage == 0 && lastOpenedAt == null && now - createdAt < NEW_BOOK_WINDOW_MILLIS

/** Finished, with the progress moved to the last page when the page count is known. */
fun Book.markedAsRead(now: Long = System.currentTimeMillis()): Book =
    withStatus(BookStatus.FINISHED, now).copy(currentPage = totalPages ?: currentPage)

/** The book with [status]; finishing it records when ([now]), and leaving "Terminado" forgets it. */
fun Book.withStatus(status: BookStatus, now: Long = System.currentTimeMillis()): Book = copy(
    status = status,
    finishedAt = when {
        status != BookStatus.FINISHED -> null
        this.status == BookStatus.FINISHED -> finishedAt
        else -> now
    },
)

/**
 * The book "Continuar leyendo" opens: the digital book read most recently, unless it is finished.
 * Null when no book was opened in the reader yet.
 */
fun List<Book>.bookToContinue(): Book? =
    filter { it.kind == BookKind.DIGITAL && it.status != BookStatus.FINISHED && it.lastOpenedAt != null }
        .maxByOrNull { it.lastOpenedAt ?: 0L }
