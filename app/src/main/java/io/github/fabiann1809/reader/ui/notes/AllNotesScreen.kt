package io.github.fabiann1809.reader.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.NoteItem
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage

@Composable
fun AllNotesScreen(
    onNavigateUp: () -> Unit,
    onNoteClick: (Note) -> Unit,
    viewModel: AllNotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AllNotesContent(uiState = uiState, onNavigateUp = onNavigateUp, onNoteClick = onNoteClick)
}

@Composable
fun AllNotesContent(
    uiState: AllNotesUiState,
    onNavigateUp: () -> Unit,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.all_notes_title), onNavigateUp = onNavigateUp) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.notes.isEmpty() -> Box(contentModifier, contentAlignment = Alignment.Center) {
                StatusMessage(
                    icon = R.drawable.ic_note_pencil,
                    title = stringResource(R.string.notes_empty),
                    message = stringResource(R.string.notes_empty_message),
                )
            }
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.notes, key = { it.note.id }) { item ->
                    NoteItem(note = item.note, bookTitle = item.bookTitle, onClick = { onNoteClick(item.note) })
                }
            }
        }
    }
}
