package io.github.fabiann1809.reader.ui.extractedtext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.MAX_TEXT_LENGTH
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
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
            .padding(16.dp)
        when (uiState) {
            ExtractedTextUiState.Recognizing -> Recognizing(contentModifier)
            is ExtractedTextUiState.Failed -> OcrFailed(uiState.reason, onRetake = onNavigateUp, modifier = contentModifier)
            is ExtractedTextUiState.Editing -> TextEditor(uiState, onTextChange, onExplain, modifier = contentModifier)
        }
    }
}

/** Skeleton lines where the text will appear (design 7.21), with what is happening below. */
@Composable
private fun Recognizing(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(stringResource(R.string.extracted_text_label))
        val skeleton = MaterialTheme.colorScheme.surfaceContainer
        listOf(1f, 0.92f, 0.97f, 0.6f).forEach { width ->
            Box(
                Modifier
                    .fillMaxWidth(width)
                    .height(16.dp)
                    .background(skeleton, MaterialTheme.shapes.small),
            )
        }
        Text(
            text = stringResource(R.string.extracted_text_recognizing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
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
        modifier = modifier.imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel(stringResource(R.string.extracted_text_label))
        Text(
            text = stringResource(R.string.extracted_text_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            shape = readerTextFieldShape,
            colors = readerTextFieldColors(),
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
        AiButton(
            text = stringResource(R.string.extracted_text_explain),
            onClick = { onExplain(state.text) },
            enabled = state.canContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

/** Warning banner (design: "OCR dudoso") plus a way back to the camera. */
@Composable
private fun OcrFailed(reason: OcrFailure, onRetake: () -> Unit, modifier: Modifier = Modifier) {
    val warning = ReaderTheme.colors.warning
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(warning.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
                .border(1.dp, warning, MaterialTheme.shapes.medium)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_warning_circle),
                contentDescription = null,
                tint = warning,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(
                    when (reason) {
                        OcrFailure.NO_TEXT_FOUND -> R.string.extracted_text_no_text
                        OcrFailure.UNREADABLE_IMAGE -> R.string.extracted_text_unreadable
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlineButton(
            text = stringResource(R.string.extracted_text_retake),
            onClick = onRetake,
            icon = R.drawable.ic_camera,
            modifier = Modifier.fillMaxWidth(),
        )
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
