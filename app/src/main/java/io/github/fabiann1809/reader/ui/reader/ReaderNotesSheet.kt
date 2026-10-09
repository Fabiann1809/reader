package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.bookdetail.HighlightItem
import io.github.fabiann1809.reader.ui.components.NoteItem
import io.github.fabiann1809.reader.ui.components.StatusMessage

/**
 * "Notas" in the reader: the book's notes and highlights without leaving the page. A note or highlight
 * with a place goes there; a note without one opens its editor.
 */
@Composable
fun ReaderNotesSheet(
    onGoTo: (location: String) -> Unit,
    onOpenNote: (noteId: Long) -> Unit,
    onDismiss: () -> Unit,
    viewModel: ReaderNotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderNotesContent(uiState, onGoTo, onOpenNote, onDismiss)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderNotesContent(
    uiState: ReaderNotesUiState,
    onGoTo: (location: String) -> Unit,
    onOpenNote: (noteId: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.reader_notes_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        if (!uiState.isLoading && uiState.notes.isEmpty() && uiState.highlights.isEmpty()) {
            StatusMessage(
                icon = R.drawable.ic_note_pencil,
                title = stringResource(R.string.notes_empty),
                message = stringResource(R.string.reader_notes_empty_message),
            )
            return@ModalBottomSheet
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            items(uiState.notes, key = { "note_${it.id}" }) { note ->
                NoteRow(note, onGoTo, onOpenNote)
            }
            if (uiState.highlights.isNotEmpty()) {
                item(key = "highlights_title") {
                    Text(
                        text = stringResource(R.string.highlights_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(uiState.highlights, key = { "highlight_${it.id}" }) { highlight ->
                    HighlightRow(highlight, onGoTo)
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onGoTo: (String) -> Unit, onOpenNote: (Long) -> Unit) {
    NoteItem(
        note = note,
        onClick = { onOpenNote(note.id) },
        onOpenInBook = note.location?.let { location -> { onGoTo(location) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun HighlightRow(highlight: Highlight, onGoTo: (String) -> Unit) {
    HighlightItem(highlight, modifier = Modifier.fillMaxWidth(), onClick = { onGoTo(highlight.location) })
}
