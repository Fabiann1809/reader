package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.collection.LibraryFilter

/**
 * The library bar in its three modes: selecting books, searching, or the header with the
 * collection selector, search, "Vista, orden y filtros", the collection menu and the chips.
 */
@Composable
fun LibraryTopBar(
    uiState: LibraryUiState,
    isSearchOpen: Boolean,
    onSearchOpenChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onOpenDialog: (LibraryDialog) -> Unit,
    onOpenSelectionDialog: (SelectionDialog) -> Unit,
    onClearSelection: () -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
) {
    // Opaque, so the list that scrolls under the header does not show through it.
    Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
        when {
            uiState.isSelecting -> SelectionTopBar(
                count = uiState.selectedIds.size,
                onClose = onClearSelection,
                onAddToCollection = { onOpenSelectionDialog(SelectionDialog.COLLECTION) },
                onDelete = { onOpenSelectionDialog(SelectionDialog.DELETE) },
            )
            isSearchOpen -> LibrarySearchBar(
                query = uiState.query,
                resultCount = uiState.books.size,
                onQueryChange = onSearch,
                onClose = {
                    onSearchOpenChange(false)
                    onSearch("")
                },
            )
            else -> LibraryHeader(
                uiState = uiState,
                onOpenSearch = { onSearchOpenChange(true) },
                onOpenDialog = onOpenDialog,
                onSelectFilter = onSelectFilter,
            )
        }
    }
}

@Composable
fun CollectionMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_dots_three_vertical),
                contentDescription = stringResource(R.string.more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.collection_rename)) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.collection_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}
