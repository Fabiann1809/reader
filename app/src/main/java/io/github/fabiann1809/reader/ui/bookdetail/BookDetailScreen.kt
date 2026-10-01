package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.NoteItem
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.ui.components.bookProgressText
import io.github.fabiann1809.reader.ui.components.labelRes
import io.github.fabiann1809.reader.ui.components.progressFraction
import io.github.fabiann1809.reader.ui.library.CollectionNameDialog
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.formatDate
import kotlin.math.roundToInt

@Composable
fun BookDetailScreen(
    onNavigateUp: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onCapturePage: () -> Unit,
    onRead: () -> Unit = {},
    viewModel: BookDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()

    LaunchedEffect(isDeleted) {
        if (isDeleted) onNavigateUp()
    }

    BookDetailContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onUpdateProgress = viewModel::updateProgress,
        onDeleteBook = viewModel::deleteBook,
        onFavoriteChange = viewModel::setFavorite,
        onCollectionChange = viewModel::setInCollection,
        onCreateCollection = viewModel::createCollectionWithBook,
        onAddNote = onAddNote,
        onNoteClick = onNoteClick,
        onCapturePage = onCapturePage,
        onRead = onRead,
    )
}

@Composable
fun BookDetailContent(
    uiState: BookDetailUiState,
    onNavigateUp: () -> Unit,
    onUpdateProgress: (currentPage: Int, status: BookStatus) -> Unit,
    onDeleteBook: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onCollectionChange: (collectionId: Long, isIncluded: Boolean) -> Unit,
    onCreateCollection: (String) -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onCapturePage: () -> Unit,
    modifier: Modifier = Modifier,
    onRead: () -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showProgressDialog by rememberSaveable { mutableStateOf(false) }
    var showCollectionSheet by rememberSaveable { mutableStateOf(false) }
    var showNewCollectionDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(
                title = "",
                onNavigateUp = onNavigateUp,
                actions = {
                    if (uiState is BookDetailUiState.Success) {
                        MoreMenu(
                            onAddToCollectionClick = { showCollectionSheet = true },
                            onDeleteClick = { showDeleteDialog = true },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            BookDetailUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            BookDetailUiState.NotFound -> Box(contentModifier, contentAlignment = Alignment.Center) {
                StatusMessage(
                    icon = R.drawable.ic_book,
                    title = stringResource(R.string.book_not_found),
                    message = stringResource(R.string.book_not_found_message),
                    action = {
                        OutlineButton(text = stringResource(R.string.book_back_to_library), onClick = onNavigateUp)
                    },
                )
            }
            is BookDetailUiState.Success -> BookDetailBody(
                book = uiState.book,
                notes = uiState.notes,
                onUpdateProgressClick = { showProgressDialog = true },
                onCapturePage = onCapturePage,
                onRead = onRead,
                onAddNote = onAddNote,
                onNoteClick = onNoteClick,
                modifier = contentModifier,
            )
        }
    }

    if (uiState !is BookDetailUiState.Success) return

    if (showProgressDialog) {
        UpdateProgressDialog(
            book = uiState.book,
            onConfirm = { page, status ->
                onUpdateProgress(page, status)
                showProgressDialog = false
            },
            onDismiss = { showProgressDialog = false },
        )
    }

    if (showCollectionSheet) {
        AddToCollectionSheet(
            isFavorite = uiState.book.isFavorite,
            collections = uiState.collections,
            collectionIds = uiState.collectionIds,
            onFavoriteChange = onFavoriteChange,
            onCollectionChange = onCollectionChange,
            onNewCollection = { showNewCollectionDialog = true },
            onDismiss = { showCollectionSheet = false },
        )
    }

    // Shown over the sheet, so the new collection appears checked when the dialog closes.
    if (showNewCollectionDialog) {
        CollectionNameDialog(
            title = R.string.collection_new,
            confirm = R.string.collection_create,
            initialName = "",
            onConfirm = { name ->
                onCreateCollection(name)
                showNewCollectionDialog = false
            },
            onDismiss = { showNewCollectionDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_book_title)) },
            text = { Text(stringResource(R.string.delete_book_message, uiState.book.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteBook()
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MoreMenu(onAddToCollectionClick: () -> Unit, onDeleteClick: () -> Unit) {
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
                text = { Text(stringResource(R.string.collection_add_title)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_books), contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToCollectionClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_book_title), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_trash), contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    expanded = false
                    onDeleteClick()
                },
            )
        }
    }
}

@Composable
private fun BookDetailBody(
    book: Book,
    notes: List<Note>,
    onUpdateProgressClick: () -> Unit,
    onCapturePage: () -> Unit,
    onRead: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "book") {
            BookHeader(book = book)
        }
        item(key = "actions") {
            BookDetailActions(
                book = book,
                onRead = onRead,
                onCapturePage = onCapturePage,
                onUpdateProgressClick = onUpdateProgressClick,
                onAddNote = onAddNote,
            )
        }
        item(key = "notes_header") {
            Text(
                text = stringResource(R.string.notes_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (notes.isEmpty()) {
            item(key = "notes_empty") {
                EmptyNotes()
            }
        } else {
            items(notes, key = { it.id }) { note ->
                NoteItem(note = note, onClick = { onNoteClick(note.id) })
            }
        }
    }
}

/** Cover, title and reading progress, centered as in the design (10.2). */
@Composable
private fun BookHeader(book: Book, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BookCover(book = book, titleSize = 18.sp, modifier = Modifier.width(COVER_WIDTH))
        Spacer(Modifier.height(4.dp))
        Text(text = book.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        val status = stringResource(book.status.labelRes())
        Text(
            // Books imported without metadata have no author: show only the status.
            text = if (book.author.isBlank()) status else stringResource(R.string.book_meta, book.author, status),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        ReadingProgress(book)
    }
}

@Composable
private fun ReadingProgress(book: Book) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        book.progressFraction()?.let { fraction ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { fraction },
                    color = ReaderTheme.colors.progress,
                    trackColor = MaterialTheme.colorScheme.outline,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                )
                Text(
                    text = stringResource(R.string.book_progress_percent, (fraction * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = listOfNotNull(
                bookProgressText(book),
                stringResource(R.string.book_added_on, formatDate(book.createdAt)),
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyNotes() {
    StatusMessage(
        icon = R.drawable.ic_note_pencil,
        title = stringResource(R.string.notes_empty),
        message = stringResource(R.string.notes_empty_message),
    )
}

private val COVER_WIDTH = 132.dp

@Preview(showBackground = true)
@Composable
private fun BookDetailPreview() {
    ReaderTheme {
        BookDetailContent(
            uiState = BookDetailUiState.Success(
                book = Book(
                    id = 1,
                    title = "Cosmos",
                    author = "Carl Sagan",
                    currentPage = 120,
                    totalPages = 400,
                    status = BookStatus.READING,
                ),
                notes = listOf(
                    Note(id = 1, bookId = 1, page = 12, content = "Somos polvo de estrellas."),
                    Note(
                        id = 2,
                        bookId = 1,
                        content = "Idea central: el universo es enorme y antiguo.",
                        type = NoteType.EXPLANATION,
                    ),
                ),
            ),
            onNavigateUp = {},
            onUpdateProgress = { _, _ -> },
            onDeleteBook = {},
            onFavoriteChange = {},
            onCollectionChange = { _, _ -> },
            onCreateCollection = {},
            onAddNote = {},
            onNoteClick = {},
            onCapturePage = {},
        )
    }
}
