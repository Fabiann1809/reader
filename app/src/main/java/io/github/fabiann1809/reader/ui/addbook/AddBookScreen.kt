package io.github.fabiann1809.reader.ui.addbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.components.BookCover
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun AddBookScreen(
    onNavigateUp: () -> Unit,
    viewModel: AddBookViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Leave the screen once the book is stored.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateUp()
    }

    AddBookContent(
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onAuthorChange = viewModel::onAuthorChange,
        onTotalPagesChange = viewModel::onTotalPagesChange,
        onSave = viewModel::save,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun AddBookContent(
    uiState: AddBookUiState,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTotalPagesChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            AddBookHeader(onNavigateUp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CoverPreview(uiState)
                OutlinedTextField(
                    shape = readerTextFieldShape,
                    colors = readerTextFieldColors(),
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.book_field_title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    shape = readerTextFieldShape,
                    colors = readerTextFieldColors(),
                    value = uiState.author,
                    onValueChange = onAuthorChange,
                    label = { Text(stringResource(R.string.book_field_author)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    shape = readerTextFieldShape,
                    colors = readerTextFieldColors(),
                    value = uiState.totalPages,
                    onValueChange = onTotalPagesChange,
                    label = { Text(stringResource(R.string.book_field_total_pages)) },
                    supportingText = {
                        val text = if (uiState.isTotalPagesValid) R.string.field_optional else R.string.book_pages_invalid
                        Text(stringResource(text))
                    },
                    isError = !uiState.isTotalPagesValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PrimaryButton(
                text = stringResource(R.string.add_book_save),
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun AddBookHeader(onNavigateUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onNavigateUp),
        ) {
            Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.navigate_up))
        }
        Text(stringResource(R.string.add_book_title), style = MaterialTheme.typography.headlineSmall)
    }
}

/** The cover as the library will draw it, changing with the title while it is typed. */
@Composable
private fun CoverPreview(uiState: AddBookUiState) {
    val preview = Book(
        title = uiState.title.trim().ifEmpty { stringResource(R.string.add_book_preview_title) },
        author = uiState.author.trim(),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        BookCover(book = preview, titleSize = 20.sp, modifier = Modifier.width(120.dp))
        Text(
            text = stringResource(R.string.add_book_cover_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddBookContentPreview() {
    ReaderTheme {
        AddBookContent(
            uiState = AddBookUiState(title = "Cosmos", author = "Carl Sagan"),
            onTitleChange = {},
            onAuthorChange = {},
            onTotalPagesChange = {},
            onSave = {},
            onNavigateUp = {},
        )
    }
}
