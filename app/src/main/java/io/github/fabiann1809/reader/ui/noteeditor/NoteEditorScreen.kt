package io.github.fabiann1809.reader.ui.noteeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun NoteEditorScreen(
    onNavigateUp: () -> Unit,
    // "Crear ficha" on a saved note (T14.2): its text, page and label.
    onCreateFlashcard: (source: String, page: Int?, tag: NoteTag?) -> Unit = { _, _, _ -> },
    viewModel: NoteEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) onNavigateUp()
    }

    NoteEditorContent(
        uiState = uiState,
        onContentChange = viewModel::onContentChange,
        onPageChange = viewModel::onPageChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onNavigateUp = onNavigateUp,
        onCreateFlashcard = { onCreateFlashcard(uiState.flashcardSource, uiState.page.toIntOrNull(), uiState.tag) },
    )
}

@Composable
fun NoteEditorContent(
    uiState: NoteEditorUiState,
    onContentChange: (String) -> Unit,
    onPageChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateFlashcard: () -> Unit = {},
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(
                title = stringResource(if (uiState.isEditing) R.string.note_edit_title else R.string.note_new_title),
                onNavigateUp = onNavigateUp,
                actions = {
                    if (uiState.isEditing && !uiState.isLoading) {
                        IconButton(onClick = onCreateFlashcard, enabled = uiState.content.isNotBlank()) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cards_three),
                                contentDescription = stringResource(R.string.flashcard_create),
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash),
                                contentDescription = stringResource(R.string.delete_note_title),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            NoteForm(
                uiState = uiState,
                onContentChange = onContentChange,
                onPageChange = onPageChange,
                onSave = onSave,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
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
private fun NoteForm(
    uiState: NoteEditorUiState,
    onContentChange: (String) -> Unit,
    onPageChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        uiState.quote?.let { Quote(it) }
        OutlinedTextField(
            shape = readerTextFieldShape,
            colors = readerTextFieldColors(),
            value = uiState.content,
            onValueChange = onContentChange,
            label = { Text(stringResource(R.string.note_field_content)) },
            minLines = CONTENT_MIN_LINES,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            shape = readerTextFieldShape,
            colors = readerTextFieldColors(),
            value = uiState.page,
            onValueChange = onPageChange,
            label = { Text(stringResource(R.string.note_field_page)) },
            supportingText = {
                val text = if (uiState.isPageValid) R.string.field_optional else R.string.book_pages_invalid
                Text(stringResource(text))
            },
            isError = !uiState.isPageValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = uiState.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val CONTENT_MIN_LINES = 6

@Preview(showBackground = true)
@Composable
private fun NoteEditorPreview() {
    ReaderTheme {
        NoteEditorContent(
            uiState = NoteEditorUiState(isEditing = true, content = "Somos polvo de estrellas.", page = "12"),
            onContentChange = {},
            onPageChange = {},
            onSave = {},
            onDelete = {},
            onNavigateUp = {},
        )
    }
}
