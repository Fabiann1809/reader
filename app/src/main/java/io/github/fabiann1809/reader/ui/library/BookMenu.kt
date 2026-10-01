package io.github.fabiann1809.reader.ui.library

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookStatus

/** What the cover menu can do; each action receives the book's id. */
class BookMenuActions(
    val onOpen: (Long) -> Unit,
    val onDetail: (Long) -> Unit,
    val onCollection: (Long) -> Unit,
    val onMarkAsRead: (Long) -> Unit,
    val onDelete: (Long) -> Unit,
)

/** Long-press menu of a cover (design 6.4): Abrir · Detalle · Colección · Marcar como leído · Eliminar. */
@Composable
fun BookMenu(book: Book, expanded: Boolean, onDismiss: () -> Unit, actions: BookMenuActions) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        @Composable
        fun item(text: String, action: (Long) -> Unit, isDestructive: Boolean = false) = DropdownMenuItem(
            text = { Text(text, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
            onClick = {
                onDismiss()
                action(book.id)
            },
        )
        // Paper books are read outside the app, so only digital ones can be opened.
        if (book.kind == BookKind.DIGITAL) item(stringResource(R.string.book_menu_open), actions.onOpen)
        item(stringResource(R.string.book_menu_detail), actions.onDetail)
        item(stringResource(R.string.book_menu_collection), actions.onCollection)
        if (book.status != BookStatus.FINISHED) item(stringResource(R.string.book_menu_mark_read), actions.onMarkAsRead)
        item(stringResource(R.string.action_delete), actions.onDelete, isDestructive = true)
    }
}
