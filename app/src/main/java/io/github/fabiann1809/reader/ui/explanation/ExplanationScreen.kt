package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun ExplanationScreen(
    onNavigateUp: () -> Unit,
    onNoteSaved: () -> Unit,
    viewModel: ExplanationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saveState) {
        if (uiState.saveState == SaveState.SAVED) onNoteSaved()
    }
    ExplanationContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        onSaveAsNote = viewModel::saveAsNote,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun ExplanationContent(
    uiState: ExplanationUiState,
    onRetry: () -> Unit,
    onSaveAsNote: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(
                title = stringResource(R.string.explanation_title),
                onNavigateUp = onNavigateUp,
                actions = { AiGeneratedChip(Modifier.padding(end = 12.dp)) },
            )
        },
    ) { innerPadding ->
        ExplanationBody(
            uiState = uiState,
            onRetry = onRetry,
            onSaveAsNote = onSaveAsNote,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * The explanation itself: the original text (collapsed), then loading, the blocks or the error.
 * Shared by this screen and the reader's explainer sheet (T11.11); the caller makes it scroll.
 */
@Composable
fun ExplanationBody(
    uiState: ExplanationUiState,
    onRetry: () -> Unit,
    onSaveAsNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SourceText(uiState.sourceText)
        when (val state = uiState.explanation) {
            ExplanationState.Loading -> Loading()
            is ExplanationState.Success -> ExplanationBlocks(state.explanation, uiState, onSaveAsNote)
            is ExplanationState.Failed -> Failed(state.error, onRetry)
        }
    }
}

/** Every piece of AI content carries this label (design rule: transparency). */
@Composable
fun AiGeneratedChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(ReaderTheme.colors.aiSoft, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = ReaderTheme.colors.ai,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.explanation_ai_label),
            style = MaterialTheme.typography.labelMedium,
            color = ReaderTheme.colors.ai,
        )
    }
}

/** The original fragment, collapsed by default so the explanation comes first. */
@Composable
private fun SourceText(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_caret_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (expanded) 0f else -90f),
            )
            Text(
                text = stringResource(R.string.explanation_source_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(16.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ExplanationBlocks(explanation: Explanation, uiState: ExplanationUiState, onSaveAsNote: () -> Unit) {
    // Selectable so the user can copy parts of the explanation.
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            explanation.caveat?.let { CaveatBlock(it) }
            MainIdeaBlock(explanation.mainIdea)
            SimpleExplanationBlock(explanation.simpleExplanation)
            AnalogyBlock(explanation.analogy)
        }
    }
    KeyTermsBlock(explanation.keyTerms)
    PrimaryButton(
        text = stringResource(
            if (uiState.saveState == SaveState.SAVED) R.string.explanation_saved_note else R.string.explanation_save_note,
        ),
        onClick = onSaveAsNote,
        enabled = uiState.canSave,
        icon = R.drawable.ic_bookmark_simple,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    )
}

/** A gently pulsing sparkle while the AI "thinks" (design motion), plus skeleton lines. */
@Composable
private fun Loading() {
    val pulse by rememberInfiniteTransition(label = "aiPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "aiPulseScale",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = ReaderTheme.colors.ai,
            modifier = Modifier
                .size(48.dp)
                .scale(pulse),
        )
        Text(stringResource(R.string.explanation_loading), style = MaterialTheme.typography.titleSmall)
        listOf(1f, 0.85f, 0.65f).forEach { width ->
            Box(
                Modifier
                    .fillMaxWidth(width)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small),
            )
        }
    }
}

/** Calm error state; no connection gets its own icon and title because it is the most common case. */
@Composable
private fun Failed(error: Throwable, onRetry: () -> Unit) {
    val offline = error is AiError.NoInternet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(if (offline) R.drawable.ic_cloud_slash else R.drawable.ic_warning_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(if (offline) R.string.explanation_offline_title else R.string.explanation_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(aiErrorMessageRes(error)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.explanation_retry),
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

private const val PULSE_MILLIS = 600

@Preview(showBackground = true)
@Composable
private fun ExplanationPreview() {
    ReaderTheme {
        ExplanationContent(
            uiState = ExplanationUiState(
                sourceText = "La entropía es una medida del desorden de un sistema.",
                explanation = ExplanationState.Success(
                    Explanation(
                        mainIdea = "El desorden siempre tiende a aumentar.",
                        simpleExplanation = "Las cosas se desordenan solas y no vuelven a ordenarse sin esfuerzo.",
                        analogy = "Como un castillo de arena que las olas deshacen.",
                        keyTerms = listOf(KeyTerm("Entropía", "Medida del desorden de un sistema.")),
                    ),
                ),
            ),
            onRetry = {},
            onSaveAsNote = {},
            onNavigateUp = {},
        )
    }
}
