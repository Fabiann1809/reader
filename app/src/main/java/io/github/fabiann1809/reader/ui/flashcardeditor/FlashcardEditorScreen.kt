package io.github.fabiann1809.reader.ui.flashcardeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.explanation.AiGeneratedChip
import io.github.fabiann1809.reader.ui.noteeditor.Quote
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun FlashcardEditorScreen(
    onNavigateUp: () -> Unit,
    viewModel: FlashcardEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateUp()
    }

    FlashcardEditorContent(
        uiState = uiState,
        onFrontChange = viewModel::onFrontChange,
        onBackChange = viewModel::onBackChange,
        onPageChange = viewModel::onPageChange,
        onSuggest = viewModel::suggest,
        onSave = viewModel::save,
        onNavigateUp = onNavigateUp,
    )
}

/** "Nueva ficha" (T14.2): the source it comes from, "Sugerir con IA", front, back and page. */
@Composable
fun FlashcardEditorContent(
    uiState: FlashcardEditorUiState,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onPageChange: (String) -> Unit,
    onSuggest: () -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.flashcard_new_title), onNavigateUp = onNavigateUp) },
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
            uiState.source?.let { source ->
                Quote(source)
                AiButton(
                    text = stringResource(if (uiState.isSuggesting) R.string.flashcard_suggesting else R.string.flashcard_suggest),
                    onClick = onSuggest,
                    enabled = uiState.canSuggest,
                    modifier = Modifier.fillMaxWidth(),
                )
                uiState.suggestionError?.let { error ->
                    Text(
                        text = stringResource(aiErrorMessageRes(error)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (uiState.isAiSuggested) AiGeneratedChip()
            CardField(uiState.front, onFrontChange, R.string.flashcard_field_front, FRONT_MIN_LINES)
            CardField(uiState.back, onBackChange, R.string.flashcard_field_back, BACK_MIN_LINES)
            OutlinedTextField(
                shape = readerTextFieldShape,
                colors = readerTextFieldColors(),
                value = uiState.page,
                onValueChange = onPageChange,
                label = { Text(stringResource(R.string.note_field_page)) },
                supportingText = {
                    Text(stringResource(if (uiState.isPageValid) R.string.field_optional else R.string.book_pages_invalid))
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
}

@Composable
private fun CardField(value: String, onChange: (String) -> Unit, label: Int, minLines: Int) {
    OutlinedTextField(
        shape = readerTextFieldShape,
        colors = readerTextFieldColors(),
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val FRONT_MIN_LINES = 2
private const val BACK_MIN_LINES = 4

@Preview(showBackground = true)
@Composable
private fun FlashcardEditorPreview() {
    ReaderTheme {
        FlashcardEditorContent(
            uiState = FlashcardEditorUiState(
                source = "La entropía es una medida del desorden de un sistema.",
                front = "¿Qué mide la entropía?",
                back = "El desorden de un sistema.",
                isAiSuggested = true,
            ),
            onFrontChange = {},
            onBackChange = {},
            onPageChange = {},
            onSuggest = {},
            onSave = {},
            onNavigateUp = {},
        )
    }
}
