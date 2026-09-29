package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.bookProgressText
import io.github.fabiann1809.reader.ui.components.labelRes
import io.github.fabiann1809.reader.ui.components.progressFraction
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.formatDate

@Composable
fun BookDetailScreen(
    onNavigateUp: () -> Unit,
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
    )
}

@Composable
fun BookDetailContent(
    uiState: BookDetailUiState,
    onNavigateUp: () -> Unit,
    onUpdateProgress: (currentPage: Int, status: BookStatus) -> Unit,
    onDeleteBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showProgressDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(
                title = stringResource(R.string.book_detail_title),
                onNavigateUp = onNavigateUp,
                actions = {
                    if (uiState is BookDetailUiState.Success) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_book_title),
                            )
                        }
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
            BookDetailUiState.NotFound -> Box(contentModifier.padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.book_not_found))
            }
            is BookDetailUiState.Success -> BookDetailBody(
                book = uiState.book,
                notes = uiState.notes,
                onUpdateProgressClick = { showProgressDialog = true },
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
private fun BookDetailBody(
    book: Book,
    notes: List<Note>,
    onUpdateProgressClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "book") {
            BookInfo(book = book, onUpdateProgressClick = onUpdateProgressClick)
        }
        item(key = "notes_header") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(text = stringResource(R.string.notes_title), style = MaterialTheme.typography.titleLarge)
        }
        if (notes.isEmpty()) {
            item(key = "notes_empty") {
                Text(
                    text = stringResource(R.string.notes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(notes, key = { it.id }) { note ->
                NoteItem(note = note)
            }
        }
    }
}

@Composable
private fun BookInfo(book: Book, onUpdateProgressClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = book.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = book.author,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(book.status.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = bookProgressText(book), style = MaterialTheme.typography.bodyLarge)
        book.progressFraction()?.let { fraction ->
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = stringResource(R.string.book_added_on, formatDate(book.createdAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onUpdateProgressClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.update_progress_title))
        }
    }
}

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
        )
    }
}
