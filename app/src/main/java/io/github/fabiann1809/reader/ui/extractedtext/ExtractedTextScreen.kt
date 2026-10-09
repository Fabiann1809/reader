package io.github.fabiann1809.reader.ui.extractedtext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding()) {
            Header(onNavigateUp)
            val contentModifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            when (uiState) {
                ExtractedTextUiState.Recognizing -> Recognizing(contentModifier)
                is ExtractedTextUiState.Failed -> OcrFailed(uiState.reason, onRetake = onNavigateUp, modifier = contentModifier)
                is ExtractedTextUiState.Editing -> TextEditor(uiState, onTextChange, onExplain, modifier = contentModifier)
            }
        }
    }
}

/** Back arrow and "Texto reconocido". */
@Composable
private fun Header(onNavigateUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onNavigateUp),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = stringResource(R.string.navigate_up))
        }
        Text(stringResource(R.string.extracted_text_title), style = MaterialTheme.typography.headlineSmall)
    }
}

/** Skeleton lines where the text will appear (design 7.21), with what is happening below. */
@Composable
private fun Recognizing(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text(
            text = stringResource(R.string.extracted_text_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val linesToCheck = state.linesToCheck
        if (linesToCheck.isNotEmpty()) WarningBanner(stringResource(R.string.extracted_text_uncertain))
        val warning = ReaderTheme.colors.warning
        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            visualTransformation = UncertainLinesHighlight(linesToCheck, warning.copy(alpha = UNCERTAIN_MARK_ALPHA)),
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

// Doubted lines get a light wash of the warning color behind them.
private const val UNCERTAIN_MARK_ALPHA = 0.3f

/** Warning banner of the design ("OCR dudoso"): warning color, icon and message. */
@Composable
private fun WarningBanner(message: String, modifier: Modifier = Modifier) {
    val warning = ReaderTheme.colors.warning
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ReaderTheme.colors.warningContainer, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 19.sp),
            color = ReaderTheme.colors.onWarningContainer,
        )
    }
}

/** The OCR read nothing: the warning plus a way back to the camera. */
@Composable
private fun OcrFailed(reason: OcrFailure, onRetake: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WarningBanner(
            stringResource(
                when (reason) {
                    OcrFailure.NO_TEXT_FOUND -> R.string.extracted_text_no_text
                    OcrFailure.UNREADABLE_IMAGE -> R.string.extracted_text_unreadable
                },
            ),
        )
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
