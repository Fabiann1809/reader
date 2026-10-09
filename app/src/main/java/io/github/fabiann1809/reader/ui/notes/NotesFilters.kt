package io.github.fabiann1809.reader.ui.notes

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

/** "Todas las notas" filters (T17.4): a search field and a chip per kind of note. */
@Composable
fun NotesFilters(query: String, type: NoteType?, onSearch: (String) -> Unit, onType: (NoteType?) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        OutlinedTextField(
            shape = readerTextFieldShape,
            colors = readerTextFieldColors(),
            value = query,
            onValueChange = onSearch,
            placeholder = { Text(stringResource(R.string.notes_search_hint)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_magnifying_glass), contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.library_search_clear))
                    }
                }
            },
            singleLine = true,
            // Results show while typing; the key only hides the keyboard.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TypeChip(R.string.notes_filter_all, selected = type == null) { onType(null) }
            NoteType.entries.forEach { noteType ->
                TypeChip(noteType.filterLabel(), selected = type == noteType) { onType(noteType) }
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
