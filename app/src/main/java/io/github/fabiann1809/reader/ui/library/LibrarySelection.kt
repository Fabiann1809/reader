package io.github.fabiann1809.reader.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.collection.Collection

/** What the selection bar and its sheet do with the checked books. */
class LibrarySelectionActions(
    val onToggle: (bookId: Long) -> Unit = {},
    val onClear: () -> Unit = {},
    val onAddToFavorites: () -> Unit = {},
    val onAddToCollection: (collectionId: Long) -> Unit = {},
    val onCreateCollection: (name: String) -> Unit = {},
    val onDelete: () -> Unit = {},
)

/** Library bar while selecting: how many books are checked, plus what can be done with them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(count: Int, onClose: () -> Unit, onAddToCollection: () -> Unit, onDelete: () -> Unit) {
    BackHandler(onBack = onClose)
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.selection_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.selection_close))
            }
        },
        actions = {
            IconButton(onClick = onAddToCollection) {
                Icon(painterResource(R.drawable.ic_books), contentDescription = stringResource(R.string.collection_add_title))
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_trash), contentDescription = stringResource(R.string.action_delete))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}

/**
 * Selection look of a book (design 6.4): a primary check in the corner when checked,
 * and the unchecked books dimmed. [isSelected] is null outside selection mode.
 */
@Composable
fun SelectionFrame(isSelected: Boolean?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = if (isSelected == null) modifier else modifier.semantics { selected = isSelected }) {
        Box(Modifier.alpha(if (isSelected == false) DIMMED_ALPHA else 1f)) { content() }
        if (isSelected == true) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(3.dp)
                    .size(14.dp),
            )
        }
    }
}

/** Where to add the checked books: "Mis favoritos", a user collection or a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSelectionToCollectionSheet(
    collections: List<Collection>,
    onFavorites: () -> Unit,
    onCollection: (Long) -> Unit,
    onNewCollection: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.collection_add_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SheetRow(stringResource(R.string.collection_favorites), onFavorites)
            if (collections.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                collections.forEach { collection -> SheetRow(collection.name) { onCollection(collection.id) } }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.collection_new), color = MaterialTheme.colorScheme.primary) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.clickable(role = Role.Button, onClick = onNewCollection),
            )
        }
    }
}

@Composable
private fun SheetRow(name: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
fun DeleteBooksDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_books_title)) },
        text = { Text(pluralStringResource(R.plurals.delete_books_message, count, count)) },
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

private const val DIMMED_ALPHA = 0.45f
