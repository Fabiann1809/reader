package io.github.fabiann1809.reader.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.NoteItem
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.ui.components.coverStyle
import io.github.fabiann1809.reader.ui.voice.VoiceNoteControls
import io.github.fabiann1809.reader.ui.voice.rememberVoiceNoteControls

@Composable
fun AllNotesScreen(
    onNavigateUp: () -> Unit,
    onNoteClick: (Note) -> Unit,
    // A note written on a passage opens the book there (T11.13).
    onOpenNoteInBook: (Note) -> Unit = {},
    viewModel: AllNotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AllNotesContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNoteClick = onNoteClick,
        onOpenNoteInBook = onOpenNoteInBook,
        voiceNotes = rememberVoiceNoteControls(),
        onSearch = viewModel::search,
        onType = viewModel::showType,
    )
}

/** "Todas las notas": the notes of every book, grouped by book under a search field and kind chips. */
@Composable
fun AllNotesContent(
    uiState: AllNotesUiState,
    onNavigateUp: () -> Unit,
    onNoteClick: (Note) -> Unit,
    // A note written on a passage opens the book there (T11.13).
    onOpenNoteInBook: (Note) -> Unit = {},
    modifier: Modifier = Modifier,
    voiceNotes: VoiceNoteControls = VoiceNoteControls(),
    onSearch: (String) -> Unit = {},
    onType: (NoteType?) -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding()) {
            Header(uiState, onNavigateUp)
            val contentModifier = Modifier.fillMaxSize()
            when {
                uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !uiState.hasNotes -> Box(contentModifier, contentAlignment = Alignment.Center) {
                    StatusMessage(
                        icon = R.drawable.ic_note_pencil,
                        title = stringResource(R.string.notes_empty),
                        message = stringResource(R.string.notes_empty_message),
                    )
                }
                else -> NoteGroups(uiState, onSearch, onType, onNoteClick, onOpenNoteInBook, voiceNotes, contentModifier)
            }
        }
    }
}

/** Back arrow, "Todas las notas" and how many there are. */
@Composable
private fun Header(uiState: AllNotesUiState, onNavigateUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onNavigateUp),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = stringResource(R.string.navigate_up))
        }
        Column {
            Text(stringResource(R.string.all_notes_title), style = MaterialTheme.typography.headlineSmall)
            if (uiState.hasNotes) {
                val count = uiState.notes.size
                Text(
                    pluralStringResource(R.plurals.notes_count, count, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NoteGroups(
    uiState: AllNotesUiState,
    onSearch: (String) -> Unit,
    onType: (NoteType?) -> Unit,
    onNoteClick: (Note) -> Unit,
    onOpenNoteInBook: (Note) -> Unit,
    voiceNotes: VoiceNoteControls,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "filters") {
            NotesFilters(uiState.query, uiState.type, onSearch = onSearch, onType = onType)
        }
        if (uiState.notes.isEmpty()) {
            item(key = "no_match") {
                StatusMessage(
                    icon = R.drawable.ic_magnifying_glass,
                    title = stringResource(R.string.notes_no_match),
                    message = stringResource(R.string.notes_no_match_message),
                )
            }
        }
        uiState.notes.groupBy { it.bookTitle }.forEach { (bookTitle, notes) ->
            item(key = "book:$bookTitle") { BookHeader(bookTitle, notes.size) }
            items(notes, key = { it.note.id }) { item ->
                NoteItem(
                    note = item.note,
                    onClick = { onNoteClick(item.note) },
                    onOpenInBook = { onOpenNoteInBook(item.note) },
                    isPlaying = voiceNotes.isPlaying(item.note),
                    playbackFailed = voiceNotes.failed(item.note),
                    onTogglePlayback = { voiceNotes.onToggle(item.note) },
                )
            }
        }
    }
}

/** A little spine in the book's cover colors, its title and how many notes it has. */
@Composable
private fun BookHeader(title: String, count: Int) {
    val style = coverStyle(title)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        Box(
            Modifier
                .size(width = 22.dp, height = 32.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 4.dp, bottomEnd = 4.dp, bottomStart = 2.dp))
                .background(style.cover),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(style.band),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.notes_count, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
