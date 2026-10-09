package io.github.fabiann1809.reader.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.book.importing.ImportStatus
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onAddPhysicalBook: () -> Unit,
    onOpenBook: (Long) -> Unit = onBookClick,
    viewModel: LibraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    folderViewModel: WatchedFolderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()
    val bookCollections by viewModel.bookCollections.collectAsStateWithLifecycle()
    // The system picker (SAF) shows local files plus providers like Google Drive and Dropbox.
    val pickBookFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importBooks(uris.map { it.toString() })
    }
    val watchedFolder by folderViewModel.folder.collectAsStateWithLifecycle()
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) folderViewModel.watch(uri.toString())
    }
    LibraryContent(
        uiState = uiState,
        bookCollections = bookCollections,
        bookActions = LibraryBookActions(
            onShowCollections = viewModel::showCollectionsOf,
            onHideCollections = viewModel::hideCollections,
            onFavoriteChange = viewModel::setFavorite,
            onCollectionChange = viewModel::setInCollection,
            onCreateCollection = viewModel::createCollectionWithBook,
            onMarkAsRead = viewModel::markAsRead,
            onDelete = viewModel::deleteBook,
        ),
        selectionActions = LibrarySelectionActions(
            onToggle = viewModel::toggleSelection,
            onClear = viewModel::clearSelection,
            onAddToFavorites = viewModel::addSelectedToFavorites,
            onAddToCollection = viewModel::addSelectedToCollection,
            onCreateCollection = viewModel::createCollectionWithSelected,
            onDelete = viewModel::deleteSelected,
        ),
        onBookClick = onBookClick,
        onOpenBook = onOpenBook,
        onAddPhysicalBook = onAddPhysicalBook,
        onImportFile = { pickBookFiles.launch(BOOK_MIME_TYPES) },
        watchedFolder = WatchedFolderOptions(
            folderName = watchedFolder?.name,
            onWatch = { pickFolder.launch(watchedFolder?.uri?.toUri()) },
            onStopWatching = folderViewModel::stopWatching,
        ),
        importStatus = importStatus,
        onRetryImports = viewModel::retryFailedImports,
        onDismissImportErrors = viewModel::dismissFailedImports,
        onSelectFilter = viewModel::selectFilter,
        onSearch = viewModel::search,
        onArrangementChange = viewModel::setArrangement,
        onLayoutChange = viewModel::setLayout,
        onCreateCollection = viewModel::createCollection,
        onRenameCollection = viewModel::renameCurrentCollection,
        onDeleteCollection = viewModel::deleteCurrentCollection,
    )
}

/** Puts the library together: the bar, the books, the import feedback and every sheet or dialog. */
@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    onBookClick: (Long) -> Unit,
    onAddPhysicalBook: () -> Unit,
    modifier: Modifier = Modifier,
    // "Abrir" in a digital book's menu: the reader.
    onOpenBook: (Long) -> Unit = onBookClick,
    onImportFile: () -> Unit = {},
    watchedFolder: WatchedFolderOptions = WatchedFolderOptions(),
    importStatus: ImportStatus = ImportStatus.Idle,
    onRetryImports: () -> Unit = {},
    onDismissImportErrors: () -> Unit = {},
    onSelectFilter: (LibraryFilter) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onArrangementChange: (LibraryArrangement) -> Unit = {},
    onLayoutChange: (LibraryLayout) -> Unit = {},
    onCreateCollection: (String) -> Unit = {},
    onRenameCollection: (String) -> Unit = {},
    onDeleteCollection: () -> Unit = {},
    bookCollections: BookCollections? = null,
    bookActions: LibraryBookActions = LibraryBookActions(),
    selectionActions: LibrarySelectionActions = LibrarySelectionActions(),
) {
    var dialog by rememberSaveable { mutableStateOf(LibraryDialog.NONE) }
    var selectionDialog by rememberSaveable { mutableStateOf(SelectionDialog.NONE) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var showAddBookSheet by rememberSaveable { mutableStateOf(false) }
    // Cover menu: the book whose menu is open, the one waiting for delete confirmation,
    // and the one getting a new collection.
    var menuBookId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteBookId by rememberSaveable { mutableStateOf<Long?>(null) }
    var newCollectionForBook by rememberSaveable { mutableStateOf<Long?>(null) }

    val menuActions = BookMenuActions(
        onOpen = onOpenBook,
        onDetail = onBookClick,
        onCollection = bookActions.onShowCollections,
        onMarkAsRead = bookActions.onMarkAsRead,
        onDelete = { deleteBookId = it },
        onSelect = selectionActions.onToggle,
    )
    val bookFrame = rememberBookFrame(uiState, menuBookId, onMenuDismiss = { menuBookId = null }, menuActions)
    // While selecting, every tap checks or unchecks a book instead of opening it.
    val gestures = if (uiState.isSelecting) {
        BookGestures(onClick = selectionActions.onToggle, onLongClick = selectionActions.onToggle)
    } else {
        BookGestures(onClick = onBookClick, onLongClick = { menuBookId = it })
    }
    // While importing, the progress pill takes the bottom of the screen.
    val showFab = !uiState.isLoading && uiState.books.isNotEmpty() && !uiState.isSelecting &&
        importStatus !is ImportStatus.Importing

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LibraryTopBar(
                uiState = uiState,
                isSearchOpen = isSearchOpen,
                onSearchOpenChange = { isSearchOpen = it },
                onSearch = onSearch,
                onOpenDialog = { dialog = it },
                onOpenSelectionDialog = { selectionDialog = it },
                onClearSelection = selectionActions.onClear,
                onSelectFilter = onSelectFilter,
            )
        },
        floatingActionButton = {
            if (showFab) LibraryFab(uiState.bookToContinue, onContinue = onOpenBook, onAddBook = { showAddBookSheet = true })
        },
    ) { innerPadding ->
        val empty = emptyState(uiState, { showAddBookSheet = true }, onSearch, onArrangementChange, onSelectFilter)
        Box(Modifier.fillMaxSize()) {
            LibraryBooks(
                uiState,
                innerPadding,
                empty,
                gestures,
                bookFrame,
                header = {
                    LibraryListHeader(
                        uiState = uiState,
                        onContinue = onOpenBook,
                        onOpenArrange = { dialog = LibraryDialog.ARRANGE },
                        onViewChange = { view -> onLayoutChange(uiState.layout.copy(view = view)) },
                    )
                },
            )
            ImportFeedback(importStatus, innerPadding, onRetry = onRetryImports, onDismiss = onDismissImportErrors)
        }
    }

    CollectionDialogs(
        dialog = dialog,
        onDialogChange = { dialog = it },
        uiState = uiState,
        actions = CollectionDialogActions(
            onSelectFilter = onSelectFilter,
            onArrangementChange = onArrangementChange,
            onLayoutChange = onLayoutChange,
            onCreateCollection = onCreateCollection,
            onRenameCollection = onRenameCollection,
            onDeleteCollection = onDeleteCollection,
        ),
    )
    SelectionDialogs(selectionDialog, onDialogChange = { selectionDialog = it }, uiState, selectionActions)
    BookDialogs(
        uiState = uiState,
        bookCollections = bookCollections,
        newCollectionForBook = newCollectionForBook,
        onNewCollectionForBookChange = { newCollectionForBook = it },
        deleteBookId = deleteBookId,
        onDeleteBookIdChange = { deleteBookId = it },
        actions = bookActions,
    )
    if (showAddBookSheet) {
        AddBookSheet(
            onFromFile = {
                showAddBookSheet = false
                onImportFile()
            },
            onPhysicalBook = {
                showAddBookSheet = false
                onAddPhysicalBook()
            },
            onDismiss = { showAddBookSheet = false },
            watchedFolder = WatchedFolderOptions(
                folderName = watchedFolder.folderName,
                onWatch = {
                    showAddBookSheet = false
                    watchedFolder.onWatch()
                },
                onStopWatching = {
                    showAddBookSheet = false
                    watchedFolder.onStopWatching()
                },
            ),
        )
    }
}

// Some providers report EPUB and CBZ files as generic binary data, so that type is accepted too;
// anything that is not really a supported book is rejected after reading it.
private val BOOK_MIME_TYPES = arrayOf(
    "application/epub+zip",
    "application/pdf",
    "application/vnd.comicbook+zip",
    "application/x-cbz",
    "text/plain",
    "application/octet-stream",
)

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
            onAddPhysicalBook = {},
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
            onAddPhysicalBook = {},
        )
    }
}
