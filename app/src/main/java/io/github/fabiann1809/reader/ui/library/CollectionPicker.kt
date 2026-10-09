package io.github.fabiann1809.reader.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.theme.PastelDots

@StringRes
fun SmartCollection.nameRes(): Int = when (this) {
    SmartCollection.ALL -> R.string.collection_all
    SmartCollection.FAVORITES -> R.string.collection_favorites
    SmartCollection.READING -> R.string.collection_reading
    SmartCollection.FINISHED -> R.string.collection_finished
    SmartCollection.TO_READ -> R.string.collection_to_read
}

/** Bottom sheet listing the default collections, then the user's, then "Nueva colección". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPickerSheet(
    selected: LibraryFilter,
    collections: List<Collection>,
    countOf: (LibraryFilter) -> Int,
    onSelect: (LibraryFilter) -> Unit,
    onNewCollection: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.collections_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            SmartCollection.entries.forEach { smart ->
                val filter = LibraryFilter.Smart(smart)
                CollectionRow(
                    name = stringResource(smart.nameRes()),
                    dot = PastelDots[SmartCollection.entries.indexOf(smart) % PastelDots.size],
                    count = countOf(filter),
                    isSelected = selected == filter,
                    onClick = { onSelect(filter) },
                )
            }
            collections.forEach { collection ->
                val filter = LibraryFilter.Custom(collection.id)
                CollectionRow(
                    name = collection.name,
                    dot = PastelDots[(collection.id % PastelDots.size).toInt()],
                    count = countOf(filter),
                    isSelected = selected == filter,
                    onClick = { onSelect(filter) },
                )
            }
            NewCollectionRow(onNewCollection)
        }
    }
}

/** Name input used both to create and to rename a collection. */
@Composable
fun CollectionNameDialog(
    @StringRes title: Int,
    @StringRes confirm: Int,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                label = { Text(stringResource(R.string.collection_name)) },
                singleLine = true,
                shape = readerTextFieldShape,
                colors = readerTextFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun DeleteCollectionDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_delete)) },
        text = { Text(stringResource(R.string.collection_delete_message, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private const val MAX_NAME_LENGTH = 40
