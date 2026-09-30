package io.github.fabiann1809.reader.ui.extractedtext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.MAX_TEXT_LENGTH
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun ExtractedTextScreen(
    onNavigateUp: () -> Unit,
    onExplain: (String) -> Unit,
    viewModel: ExtractedTextViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExtractedTextContent(
        uiState = uiState,
        onTextChange = viewModel::onTextChange,
        onExplain = onExplain,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun ExtractedTextContent(
    uiState: ExtractedTextUiState,
    onTextChange: (String) -> Unit,
    onExplain: (String) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.extracted_text_title), onNavigateUp = onNavigateUp)
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            ExtractedTextUiState.Recognizing -> Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.extracted_text_recognizing))
            }
            is ExtractedTextUiState.Failed -> OcrFailed(uiState.reason, onRetake = onNavigateUp, modifier = contentModifier)
            is ExtractedTextUiState.Editing -> TextEditor(uiState, onTextChange, onExplain, modifier = contentModifier)
        }
    }
}

@Composable
private fun TextEditor(
    state: ExtractedTextUiState.Editing,
    onTextChange: (String) -> Unit,
    onExplain: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.extracted_text_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            isError = state.isTooLong,
            supportingText = {
                Text(
                    text = if (state.isTooLong) {
                        stringResource(R.string.extracted_text_too_long, state.text.length, MAX_TEXT_LENGTH)
                    } else {
                        stringResource(R.string.extracted_text_counter, state.text.length, MAX_TEXT_LENGTH)
                    },
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Button(
            onClick = { onExplain(state.text) },
            enabled = state.canContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.extracted_text_explain))
        }
    }
}

@Composable
private fun OcrFailed(reason: OcrFailure, onRetake: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                when (reason) {
                    OcrFailure.NO_TEXT_FOUND -> R.string.extracted_text_no_text
                    OcrFailure.UNREADABLE_IMAGE -> R.string.extracted_text_unreadable
                },
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedButton(onClick = onRetake) {
            Text(stringResource(R.string.extracted_text_retake))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExtractedTextPreview() {
    ReaderTheme {
        ExtractedTextContent(
            uiState = ExtractedTextUiState.Editing("La entropía es una medida del desorden de un sistema."),
            onTextChange = {},
            onExplain = {},
            onNavigateUp = {},
        )
    }
}
