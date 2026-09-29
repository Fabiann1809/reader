package io.github.fabiann1809.reader.ui.noteeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun NoteEditorScreen(
    onNavigateUp: () -> Unit,
    viewModel: NoteEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateUp()
    }

    NoteEditorContent(
        uiState = uiState,
        onContentChange = viewModel::onContentChange,
        onPageChange = viewModel::onPageChange,
        onSave = viewModel::save,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun NoteEditorContent(
    uiState: NoteEditorUiState,
    onContentChange: (String) -> Unit,
    onPageChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.note_new_title), onNavigateUp = onNavigateUp)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.content,
                onValueChange = onContentChange,
                label = { Text(stringResource(R.string.note_field_content)) },
                minLines = CONTENT_MIN_LINES,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
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
            Button(onClick = onSave, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

private const val CONTENT_MIN_LINES = 6

@Preview(showBackground = true)
@Composable
private fun NoteEditorPreview() {
    ReaderTheme {
        NoteEditorContent(
            uiState = NoteEditorUiState(content = "Somos polvo de estrellas.", page = "12"),
            onContentChange = {},
            onPageChange = {},
            onSave = {},
            onNavigateUp = {},
        )
    }
}
