package io.github.fabiann1809.reader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.formatDate

// Card style from the design (7.10): surface-container, 16 dp corners, no border, elevation 1.
// A note written on a passage (T11.13) opens the book there when tapped ([onOpenInBook]) and
// is edited with its pencil ([onClick]); any other note is edited by tapping it.
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Shown where notes of several books are mixed (e.g. "Todas las notas").
    bookTitle: String? = null,
    onOpenInBook: (() -> Unit)? = null,
) {
    val openInBook = onOpenInBook.takeIf { note.location != null }
    Card(
        onClick = openInBook ?: onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookTitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (note.type == NoteType.EXPLANATION) {
                AiLabel()
            }
            if (openInBook != null) {
                AnchoredNoteHeader(onEdit = onClick)
            }
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                note.page?.let { page ->
                    Text(
                        text = stringResource(R.string.note_page, page),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatDate(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "En el libro" (tapping opens the passage) and the pencil that edits the note. */
@Composable
private fun AnchoredNoteHeader(onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_book_open_text),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.note_in_book),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
        )
        IconButton(onClick = onEdit) {
            Icon(painterResource(R.drawable.ic_note_pencil), contentDescription = stringResource(R.string.note_edit))
        }
    }
}

/** AI content is always marked with the sparkle and the lavender accent. */
@Composable
private fun AiLabel() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = ReaderTheme.colors.ai,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.note_type_explanation),
            style = MaterialTheme.typography.labelMedium,
            color = ReaderTheme.colors.ai,
        )
    }
}

private const val PREVIEW_MAX_LINES = 4
