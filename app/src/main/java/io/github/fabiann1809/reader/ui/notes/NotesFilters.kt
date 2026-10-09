package io.github.fabiann1809.reader.ui.notes

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip

/** "Todas las notas" filters (T17.4): a pill search field and a chip per kind of note. */
@Composable
fun NotesFilters(query: String, type: NoteType?, onSearch: (String) -> Unit, onType: (NoteType?) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        SearchPill(query, onSearch)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TypeChip(R.string.notes_filter_all, selected = type == null) { onType(null) }
            NoteType.entries.forEach { noteType ->
                TypeChip(noteType.filterLabel(), selected = type == noteType) { onType(noteType) }
            }
        }
    }
}

@Composable
private fun SearchPill(query: String, onSearch: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(start = 16.dp, end = 6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_magnifying_glass),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    stringResource(R.string.notes_search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                // Results show while typing; the key only hides the keyboard.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onSearch("") },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = stringResource(R.string.library_search_clear),
                    modifier = Modifier.size(20.dp).padding(0.dp),
                )
            }
        }
    }
}

@Composable
private fun TypeChip(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
    ReaderFilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(label)) },
    )
}

@StringRes
private fun NoteType.filterLabel(): Int = when (this) {
    NoteType.MANUAL -> R.string.notes_filter_written
    NoteType.EXPLANATION -> R.string.notes_filter_explanations
    NoteType.VOICE -> R.string.notes_filter_voice
}
