package io.github.fabiann1809.reader.ui.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.LightStatusBarIcons
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.Primary40
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.ceil

@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LightStatusBarIcons()
    LibraryContent(
        uiState = uiState,
        onBookClick = onBookClick,
        onAddBook = onAddBook,
        onSelectFilter = viewModel::selectFilter,
        onSearch = viewModel::search,
        onCreateCollection = viewModel::createCollection,
        onRenameCollection = viewModel::renameCurrentCollection,
        onDeleteCollection = viewModel::deleteCurrentCollection,
    )
}

@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectFilter: (LibraryFilter) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onCreateCollection: (String) -> Unit = {},
    onRenameCollection: (String) -> Unit = {},
    onDeleteCollection: () -> Unit = {},
) {
    val showFab = !uiState.isLoading && uiState.books.isNotEmpty()
    var dialog by rememberSaveable { mutableStateOf(CollectionDialog.NONE) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    val title = uiState.currentCollection?.name
        ?: stringResource((uiState.filter as? LibraryFilter.Smart)?.collection?.nameRes() ?: R.string.collection_all)
    Scaffold(
        modifier = modifier.woodWall(),
        containerColor = Color.Transparent,
        topBar = {
            if (isSearchOpen) {
                LibrarySearchBar(
                    query = uiState.query,
                    onQueryChange = onSearch,
                    onClose = {
                        isSearchOpen = false
                        onSearch("")
                    },
                )
            } else {
                // The library bar stays forest green in both themes: it is the app's identity.
                ReaderTopAppBar(
                    title = title,
                    onTitleClick = { dialog = CollectionDialog.PICKER },
                    onTitleClickLabel = stringResource(R.string.collection_change),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Primary40,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                    actions = {
                        if (!uiState.libraryIsEmpty) {
                            IconButton(onClick = { isSearchOpen = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_magnifying_glass),
                                    contentDescription = stringResource(R.string.library_search),
                                )
                            }
                        }
                        // Only collections created by the user can be renamed or deleted.
                        if (uiState.currentCollection != null) {
                            CollectionMenu(
                                onRename = { dialog = CollectionDialog.RENAME },
                                onDelete = { dialog = CollectionDialog.DELETE },
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (showFab) {
                // Content overload on purpose: the text/icon overload hides the label from screen readers.
                ExtendedFloatingActionButton(
                    onClick = onAddBook,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(painterResource(R.drawable.ic_plus_circle), contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.library_add_book))
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Bookcase(innerPadding, itemCount = PLACEHOLDER_COUNT) { _, width -> PlaceholderCover(width) }
            uiState.libraryIsEmpty -> EmptyShelf(
                contentPadding = innerPadding,
                title = stringResource(R.string.library_empty_title),
                message = stringResource(R.string.library_empty_message),
                action = stringResource(R.string.library_add_book),
                actionIcon = R.drawable.ic_plus_circle,
                onAction = onAddBook,
            )
            uiState.books.isEmpty() && uiState.query.isNotBlank() -> EmptyShelf(
                contentPadding = innerPadding,
                title = stringResource(R.string.library_search_empty_title),
                message = stringResource(R.string.library_search_empty_message, uiState.query.trim()),
                action = stringResource(R.string.library_search_clear),
                actionIcon = R.drawable.ic_x,
                onAction = { onSearch("") },
            )
            uiState.books.isEmpty() -> EmptyShelf(
                contentPadding = innerPadding,
                title = stringResource(R.string.collection_empty_title),
                message = stringResource(
                    if (uiState.currentCollection != null) {
                        R.string.collection_empty_message
                    } else {
                        R.string.collection_smart_empty_message
                    },
                ),
                action = stringResource(R.string.collection_show_all),
                actionIcon = R.drawable.ic_books,
                onAction = { onSelectFilter(LibraryFilter.Default) },
            )
            else -> Bookcase(innerPadding, itemCount = uiState.books.size, bottomSpace = FAB_SPACE) { index, width ->
                val book = uiState.books[index]
                BookCover(book = book, onClick = { onBookClick(book.id) }, modifier = Modifier.width(width))
            }
        }
    }

    when (dialog) {
        CollectionDialog.NONE -> Unit
        CollectionDialog.PICKER -> CollectionPickerSheet(
            selected = uiState.filter,
            collections = uiState.collections,
            onSelect = { filter ->
                onSelectFilter(filter)
                dialog = CollectionDialog.NONE
            },
            onNewCollection = { dialog = CollectionDialog.CREATE },
            onDismiss = { dialog = CollectionDialog.NONE },
        )
        CollectionDialog.CREATE -> CollectionNameDialog(
            title = R.string.collection_new,
            confirm = R.string.collection_create,
            initialName = "",
            onConfirm = { name ->
                onCreateCollection(name)
                dialog = CollectionDialog.NONE
            },
            onDismiss = { dialog = CollectionDialog.NONE },
        )
        CollectionDialog.RENAME -> CollectionNameDialog(
            title = R.string.collection_rename,
            confirm = R.string.collection_rename_confirm,
            initialName = uiState.currentCollection?.name.orEmpty(),
            onConfirm = { name ->
                onRenameCollection(name)
                dialog = CollectionDialog.NONE
            },
            onDismiss = { dialog = CollectionDialog.NONE },
        )
        CollectionDialog.DELETE -> DeleteCollectionDialog(
            name = uiState.currentCollection?.name.orEmpty(),
            onConfirm = {
                onDeleteCollection()
                dialog = CollectionDialog.NONE
            },
            onDismiss = { dialog = CollectionDialog.NONE },
        )
    }
}

/** Which collection sheet or dialog is open (only one at a time). */
private enum class CollectionDialog { NONE, PICKER, CREATE, RENAME, DELETE }

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

/**
 * Shelves filled with [itemCount] items, [columns] per shelf. Extra empty shelves are added
 * so the wall is always covered with shelves down to the bottom of the screen.
 */
@Composable
private fun Bookcase(
    contentPadding: PaddingValues,
    itemCount: Int,
    bottomSpace: Dp = 0.dp,
    slot: @Composable (index: Int, width: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = shelfColumns()
        val bookWidth = shelfBookWidth(maxWidth, columns)
        val rowHeight = shelfRowHeight(bookWidth)
        val shelvesWithBooks = ceil(itemCount / columns.toFloat()).toInt()
        val shelvesToFillScreen = ceil(maxHeight / rowHeight).toInt()
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + bottomSpace,
            ),
        ) {
            items(count = maxOf(shelvesWithBooks, shelvesToFillScreen)) { shelf ->
                val first = shelf * columns
                Shelf(columns = columns, bookWidth = bookWidth, itemCount = (itemCount - first).coerceAtLeast(0)) { i, width ->
                    slot(first + i, width)
                }
            }
        }
    }
}

/** 3 books per shelf; 2 when the system font is scaled up a lot, so titles stay readable. */
@Composable
private fun shelfColumns(): Int = if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) 2 else 3

@Composable
private fun PlaceholderCover(width: Dp) {
    Box(
        Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp)),
    )
}

/** Shelf with a dashed "ghost" book (design 6.6), a message and one action. */
@Composable
private fun EmptyShelf(
    contentPadding: PaddingValues,
    title: String,
    message: String,
    action: String,
    @DrawableRes actionIcon: Int,
    onAction: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val bookWidth = shelfBookWidth(maxWidth, columns = 3)
        Column {
            Shelf(columns = 3, bookWidth = bookWidth, itemCount = 1) { _, width -> GhostBook(width) }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = action, onClick = onAction, icon = actionIcon, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Dashed outline of a book: the empty-state placeholder from the design. */
@Composable
private fun GhostBook(width: Dp) {
    val outline = Color.White.copy(alpha = 0.45f)
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_book),
            contentDescription = null,
            tint = outline,
            modifier = Modifier.size(32.dp),
        )
    }
}

private const val PLACEHOLDER_COUNT = 6
private const val LARGE_FONT_SCALE = 1.3f

// Keeps the last shelf visible above the floating button.
private val FAB_SPACE = 88.dp

@Preview(showBackground = true)
@Composable
private fun LibraryWithBooksPreview() {
    ReaderTheme {
        LibraryContent(
            uiState = LibraryUiState(
                books = listOf(
                    Book(
                        id = 1,
                        title = "Cosmos",
                        author = "Carl Sagan",
                        currentPage = 120,
                        totalPages = 400,
                        status = BookStatus.READING,
                    ),
                    Book(id = 2, title = "Dune", author = "Frank Herbert"),
                ),
                isLoading = false,
            ),
            onBookClick = {},
            onAddBook = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    ReaderTheme {
        LibraryContent(
            uiState = LibraryUiState(isLoading = false),
            onBookClick = {},
            onAddBook = {},
        )
    }
}
