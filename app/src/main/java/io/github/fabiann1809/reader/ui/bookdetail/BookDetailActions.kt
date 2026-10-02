package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.TonalButton

/**
 * The detail's buttons. Only one is primary (design: never two per screen): reading for a digital
 * book ("Leer", or "Continuar" once opened), capturing a page for a paper one, which is read outside the app.
 */
@Composable
fun BookDetailActions(
    book: Book,
    onRead: () -> Unit,
    onCapturePage: () -> Unit,
    onUpdateProgressClick: () -> Unit,
    onAddNote: () -> Unit,
    onAddFlashcard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        if (book.kind == BookKind.DIGITAL) {
            PrimaryButton(
                text = stringResource(if (book.lastOpenedAt == null) R.string.book_read else R.string.book_continue),
                onClick = onRead,
                icon = R.drawable.ic_book_open_text,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.capture_title),
                onClick = onCapturePage,
                icon = R.drawable.ic_camera,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TonalButton(
                text = stringResource(R.string.update_progress_short),
                onClick = onUpdateProgressClick,
                modifier = Modifier.weight(1f),
            )
            TonalButton(
                text = stringResource(R.string.note_new_title),
                onClick = onAddNote,
                icon = R.drawable.ic_note_pencil,
                modifier = Modifier.weight(1f),
            )
        }
        // On its own row: three buttons side by side don't fit their labels on a phone.
        TonalButton(
            text = stringResource(R.string.flashcard_new),
            onClick = onAddFlashcard,
            icon = R.drawable.ic_cards_three,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
