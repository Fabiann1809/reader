package io.github.fabiann1809.reader.ui.library

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus

// UI helpers for presenting books, shared by the library and book detail screens.

@StringRes
fun BookStatus.labelRes(): Int = when (this) {
    BookStatus.TO_READ -> R.string.book_status_to_read
    BookStatus.READING -> R.string.book_status_reading
    BookStatus.FINISHED -> R.string.book_status_finished
}

@Composable
fun bookProgressText(book: Book): String = when (val total = book.totalPages) {
    null -> stringResource(R.string.book_progress, book.currentPage)
    else -> stringResource(R.string.book_progress_with_total, book.currentPage, total)
}

/** Reading progress between 0 and 1, or null when the total page count is unknown. */
fun Book.progressFraction(): Float? {
    val total = totalPages?.takeIf { it > 0 } ?: return null
    return (currentPage.toFloat() / total).coerceIn(0f, 1f)
}
