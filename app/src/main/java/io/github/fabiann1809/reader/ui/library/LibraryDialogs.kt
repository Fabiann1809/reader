package io.github.fabiann1809.reader.ui.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.ui.bookdetail.AddToCollectionSheet

/** Sheet or dialog opened from the normal library bar (only one at a time). */
enum class LibraryDialog { NONE, PICKER, CREATE, RENAME, DELETE, ARRANGE }

/** Sheet or dialog opened from the selection bar. */
enum class SelectionDialog { NONE, COLLECTION, NEW_COLLECTION, DELETE }

/** What the library bar's sheets and dialogs change. */
class CollectionDialogActions(
    val onSelectFilter: (LibraryFilter) -> Unit = {},
    val onArrangementChange: (LibraryArrangement) -> Unit = {},
    val onLayoutChange: (LibraryLayout) -> Unit = {},
    val onCreateCollection: (String) -> Unit = {},
    val onRenameCollection: (String) -> Unit = {},
    val onDeleteCollection: () -> Unit = {},
)

/** Per-book actions from the cover menu and its "Colección" sheet. */
class LibraryBookActions(
    val onShowCollections: (bookId: Long) -> Unit = {},
    val onHideCollections: () -> Unit = {},
    val onFavoriteChange: (bookId: Long, isFavorite: Boolean) -> Unit = { _, _ -> },
    val onCollectionChange: (bookId: Long, collectionId: Long, isIncluded: Boolean) -> Unit = { _, _, _ -> },
    val onCreateCollection: (bookId: Long, name: String) -> Unit = { _, _ -> },
    val onMarkAsRead: (bookId: Long) -> Unit = {},
    val onDelete: (bookId: Long) -> Unit = {},
)

/** Collection picker, "Vista, orden y filtros", and creating, renaming or deleting a collection. */
@Composable
fun CollectionDialogs(
    dialog: LibraryDialog,
    onDialogChange: (LibraryDialog) -> Unit,
    uiState: LibraryUiState,
    actions: CollectionDialogActions,
) {
    val close = { onDialogChange(LibraryDialog.NONE) }
    when (dialog) {
        LibraryDialog.NONE -> Unit
        LibraryDialog.ARRANGE -> ArrangeSheet(
            arrangement = uiState.arrangement,
            layout = uiState.layout,
            onChange = actions.onArrangementChange,
            onLayoutChange = actions.onLayoutChange,
            onDismiss = close,
        )
        LibraryDialog.PICKER -> CollectionPickerSheet(
            selected = uiState.filter,
            collections = uiState.collections,
            onSelect = { filter ->
                actions.onSelectFilter(filter)
                close()
            },
            onNewCollection = { onDialogChange(LibraryDialog.CREATE) },
            onDismiss = close,
        )
        LibraryDialog.CREATE -> CollectionNameDialog(
            title = R.string.collection_new,
            confirm = R.string.collection_create,
            initialName = "",
            onConfirm = { name ->
                actions.onCreateCollection(name)
                close()
            },
            onDismiss = close,
        )
        LibraryDialog.RENAME -> CollectionNameDialog(
            title = R.string.collection_rename,
            confirm = R.string.collection_rename_confirm,
            initialName = uiState.currentCollection?.name.orEmpty(),
            onConfirm = { name ->
                actions.onRenameCollection(name)
                close()
            },
            onDismiss = close,
        )
        LibraryDialog.DELETE -> DeleteCollectionDialog(
            name = uiState.currentCollection?.name.orEmpty(),
            onConfirm = {
                actions.onDeleteCollection()
                close()
            },
            onDismiss = close,
        )
    }
}

/** Where to add the selected books, a new collection for them, or deleting them. */
@Composable
fun SelectionDialogs(
    dialog: SelectionDialog,
    onDialogChange: (SelectionDialog) -> Unit,
    uiState: LibraryUiState,
    actions: LibrarySelectionActions,
) {
    val close = { onDialogChange(SelectionDialog.NONE) }
    when (dialog) {
        SelectionDialog.NONE -> Unit
        SelectionDialog.COLLECTION -> AddSelectionToCollectionSheet(
            collections = uiState.collections,
            onFavorites = {
                actions.onAddToFavorites()
                close()
            },
            onCollection = { collectionId ->
                actions.onAddToCollection(collectionId)
                close()
            },
            onNewCollection = { onDialogChange(SelectionDialog.NEW_COLLECTION) },
            onDismiss = close,
        )
        SelectionDialog.NEW_COLLECTION -> CollectionNameDialog(
            title = R.string.collection_new,
            confirm = R.string.collection_create,
            initialName = "",
            onConfirm = { name ->
                actions.onCreateCollection(name)
                close()
            },
            onDismiss = close,
        )
        SelectionDialog.DELETE -> DeleteBooksDialog(
            count = uiState.selectedIds.size,
            onConfirm = {
                actions.onDelete()
                close()
            },
            onDismiss = close,
        )
    }
}

/**
 * Dialogs of one book from its cover menu: the "Colección" sheet ([bookCollections] non-null),
 * a new collection for it ([newCollectionForBook]) and the delete confirmation ([deleteBookId]).
 */
@Composable
fun BookDialogs(
    uiState: LibraryUiState,
    bookCollections: BookCollections?,
    newCollectionForBook: Long?,
    onNewCollectionForBookChange: (Long?) -> Unit,
    deleteBookId: Long?,
    onDeleteBookIdChange: (Long?) -> Unit,
    actions: LibraryBookActions,
) {
    bookCollections?.let { (book, collectionIds) ->
        AddToCollectionSheet(
            isFavorite = book.isFavorite,
            collections = uiState.collections,
            collectionIds = collectionIds,
            onFavoriteChange = { actions.onFavoriteChange(book.id, it) },
            onCollectionChange = { collectionId, isIncluded -> actions.onCollectionChange(book.id, collectionId, isIncluded) },
            onNewCollection = { onNewCollectionForBookChange(book.id) },
            onDismiss = actions.onHideCollections,
        )
    }
    newCollectionForBook?.let { bookId ->
        CollectionNameDialog(
            title = R.string.collection_new,
            confirm = R.string.collection_create,
            initialName = "",
            onConfirm = { name ->
                actions.onCreateCollection(bookId, name)
                onNewCollectionForBookChange(null)
            },
            onDismiss = { onNewCollectionForBookChange(null) },
        )
    }
    deleteBookId?.let { bookId ->
        DeleteBookDialog(
            title = uiState.books.find { it.id == bookId }?.title.orEmpty(),
            onConfirm = {
                actions.onDelete(bookId)
                onDeleteBookIdChange(null)
            },
            onDismiss = { onDeleteBookIdChange(null) },
        )
    }
}

@Composable
private fun DeleteBookDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_book_title)) },
        text = { Text(stringResource(R.string.delete_book_message, title)) },
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
