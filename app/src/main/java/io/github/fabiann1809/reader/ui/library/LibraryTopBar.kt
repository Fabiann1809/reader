package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.Primary40

/**
 * The library bar in its three modes: selecting books, searching, or the normal bar with
 * the collection selector, search, "Vista, orden y filtros" and the collection menu.
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
) {
    when {
        uiState.isSelecting -> SelectionTopBar(
            count = uiState.selectedIds.size,
            onClose = onClearSelection,
            onAddToCollection = { onOpenSelectionDialog(SelectionDialog.COLLECTION) },
            onDelete = { onOpenSelectionDialog(SelectionDialog.DELETE) },
        )
        isSearchOpen -> LibrarySearchBar(
            query = uiState.query,
            onQueryChange = onSearch,
            onClose = {
                onSearchOpenChange(false)
                onSearch("")
            },
        )
        else -> CollectionTopBar(uiState, onOpenSearch = { onSearchOpenChange(true) }, onOpenDialog = onOpenDialog)
    }
}

@Composable
private fun CollectionTopBar(uiState: LibraryUiState, onOpenSearch: () -> Unit, onOpenDialog: (LibraryDialog) -> Unit) {
    val title = uiState.currentCollection?.name
        ?: stringResource((uiState.filter as? LibraryFilter.Smart)?.collection?.nameRes() ?: R.string.collection_all)
    // The library bar stays forest green in both themes: it is the app's identity.
    ReaderTopAppBar(
        title = title,
        onTitleClick = { onOpenDialog(LibraryDialog.PICKER) },
        onTitleClickLabel = stringResource(R.string.collection_change),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Primary40,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
        actions = {
            if (!uiState.libraryIsEmpty) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        painter = painterResource(R.drawable.ic_magnifying_glass),
                        contentDescription = stringResource(R.string.library_search),
                    )
                }
                IconButton(onClick = { onOpenDialog(LibraryDialog.ARRANGE) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_funnel_simple),
                        contentDescription = stringResource(R.string.library_arrange),
                    )
                }
            }
            // Only collections created by the user can be renamed or deleted.
            if (uiState.currentCollection != null) {
                CollectionMenu(
                    onRename = { onOpenDialog(LibraryDialog.RENAME) },
                    onDelete = { onOpenDialog(LibraryDialog.DELETE) },
                )
            }
        },
    )
}

@Composable
private fun CollectionMenu(onRename: () -> Unit, onDelete: () -> Unit) {
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
