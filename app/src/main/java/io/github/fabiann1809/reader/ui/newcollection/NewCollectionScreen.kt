package io.github.fabiann1809.reader.ui.newcollection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

@Composable
fun NewCollectionScreen(
    onClose: () -> Unit,
    viewModel: NewCollectionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onClose()
    }
    NewCollectionContent(
        uiState = uiState,
        onClose = onClose,
        onSave = viewModel::save,
        onNameChange = viewModel::onNameChange,
        onColorChange = viewModel::onColorChange,
        onToggleBook = viewModel::toggleBook,
    )
}

/** Preview of the collection on top, then its name, its color and the books to put in it. */
@Composable
fun NewCollectionContent(
    uiState: NewCollectionUiState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onToggleBook: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullWidth: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(BOOK_COLUMNS),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = fullWidth) { NewCollectionTopBar(uiState.canSave, onClose, onSave) }
            item(span = fullWidth) { CollectionPreview(uiState) }
            item(span = fullWidth) {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.new_collection_name)) },
                    placeholder = { Text(stringResource(R.string.new_collection_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = readerTextFieldShape,
                    colors = readerTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(span = fullWidth) { ColorSwatches(uiState.colorIndex, onColorChange) }
            item(span = fullWidth) {
                Text(
                    text = stringResource(R.string.new_collection_pick_books),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(uiState.books, key = { it.id }) { book ->
                BookChoice(
                    book = book,
                    isSelected = book.id in uiState.selectedIds,
                    onClick = { onToggleBook(book.id) },
                )
            }
        }
    }
}

private const val BOOK_COLUMNS = 3
