package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
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
    BookDetailContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onUpdateProgress = viewModel::updateProgress,
    )
}

@Composable
fun BookDetailContent(
    uiState: BookDetailUiState,
    onNavigateUp: () -> Unit,
    onUpdateProgress: (currentPage: Int, status: BookStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.book_detail_title), onNavigateUp = onNavigateUp)
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
            is BookDetailUiState.Success -> BookInfo(
                book = uiState.book,
                onUpdateProgress = onUpdateProgress,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun BookInfo(
    book: Book,
    onUpdateProgress: (currentPage: Int, status: BookStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProgressDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        OutlinedButton(onClick = { showProgressDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.update_progress_title))
        }
    }

    if (showProgressDialog) {
        UpdateProgressDialog(
            book = book,
            onConfirm = { page, status ->
                onUpdateProgress(page, status)
                showProgressDialog = false
            },
            onDismiss = { showProgressDialog = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailPreview() {
    ReaderTheme {
        BookDetailContent(
            uiState = BookDetailUiState.Success(
                Book(
                    id = 1,
                    title = "Cosmos",
                    author = "Carl Sagan",
                    currentPage = 120,
                    totalPages = 400,
                    status = BookStatus.READING,
                ),
            ),
            onNavigateUp = {},
            onUpdateProgress = { _, _ -> },
        )
    }
}
