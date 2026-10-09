package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.quiz.PARAGRAPH_QUIZ_SIZE
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun ExplanationScreen(
    onNavigateUp: () -> Unit,
    onNoteSaved: () -> Unit,
    // "Crear ficha" (T14.2), with the explained text as the card's source.
    onCreateFlashcard: (source: String) -> Unit = {},
    // "Ahora tú" (T15.1), with the explained text to write about.
    onNowYou: (source: String) -> Unit = {},
    // "Ponme a prueba": a short quiz about the explained text (source, questions, title).
    onQuiz: (source: String, count: Int, title: String) -> Unit = { _, _, _ -> },
    viewModel: ExplanationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saveState) {
        if (uiState.saveState == SaveState.SAVED) onNoteSaved()
    }
    val quizTitle = stringResource(R.string.quiz_title_paragraph)
    ExplanationContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        onSaveAsNote = viewModel::saveAsNote,
        onNavigateUp = onNavigateUp,
        onCreateFlashcard = { onCreateFlashcard(uiState.sourceText) },
        onNowYou = { onNowYou(uiState.sourceText) },
        onQuiz = { onQuiz(uiState.sourceText, PARAGRAPH_QUIZ_SIZE, quizTitle) },
    )
}

@Composable
fun ExplanationContent(
    uiState: ExplanationUiState,
    onRetry: () -> Unit,
    onSaveAsNote: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateFlashcard: (() -> Unit)? = null,
    onNowYou: (() -> Unit)? = null,
    onQuiz: (() -> Unit)? = null,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding()) {
            ExplanationHeader(onClose = onNavigateUp)
            ExplanationBody(
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ExplanationActions(uiState, onSaveAsNote, onCreateFlashcard, onNowYou, onQuiz)
        }
    }
}

/** "Explicación", the AI label and the close button: the top of the screen and of the reader's sheet. */
@Composable
fun ExplanationHeader(onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 10.dp),
    ) {
        Text(
            text = stringResource(R.string.explanation_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        AiGeneratedChip()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose),
        ) {
            Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.navigate_up), modifier = Modifier.size(22.dp))
        }
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SourceText(uiState.sourceText)
        when (val state = uiState.explanation) {
            ExplanationState.Loading -> Loading()
            is ExplanationState.Success -> ExplanationBlocks(state.explanation)
            is ExplanationState.Failed -> Failed(state.error, onRetry)
        }
    }
}

/** Every piece of AI content carries this label (design rule: transparency). */
@Composable
fun AiGeneratedChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.explanation_ai_label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
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
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_caret_right),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
            Text(
                text = stringResource(R.string.explanation_source_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        AnimatedVisibility(visible = expanded) {
            val line = MaterialTheme.colorScheme.outlineVariant
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind { drawRect(line, size = Size(2.dp.toPx(), size.height)) }
                    .padding(start = 14.dp, top = 4.dp, bottom = 14.dp),
            )
        }
    }
}

/** What can be done with the explanation, pinned under it: save, make a card, write about it, be quizzed. */
@Composable
fun ExplanationActions(
    uiState: ExplanationUiState,
    onSaveAsNote: () -> Unit,
    onCreateFlashcard: (() -> Unit)?,
    onNowYou: (() -> Unit)?,
    onQuiz: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (uiState.explanation !is ExplanationState.Success) return
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = stringResource(
                        if (uiState.saveState == SaveState.SAVED) R.string.explanation_saved_note else R.string.explanation_save_note,
                    ),
                    onClick = onSaveAsNote,
                    enabled = uiState.canSave,
                    icon = R.drawable.ic_bookmark_simple,
                    modifier = Modifier.weight(1f),
                )
                onCreateFlashcard?.let {
                    OutlineButton(
                        text = stringResource(R.string.flashcard_create),
                        onClick = it,
                        icon = R.drawable.ic_cards_three,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (onNowYou != null || onQuiz != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    onNowYou?.let {
                        AiButton(stringResource(R.string.interpretation_title), it, Modifier.weight(1f), icon = null)
                    }
                    onQuiz?.let {
                        AiButton(stringResource(R.string.quiz_try_me), it, Modifier.weight(1f), icon = null)
                    }
                }
            }
        }
    }
}

/** Gray blocks that shimmer where the explanation will appear, with a pulsing sparkle under them. */
@Composable
private fun Loading() {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "aiLoading")
    val shimmer by transition.animateFloat(
        initialValue = -SHIMMER_SPAN,
        targetValue = SHIMMER_SPAN,
        animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "aiPulse",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val light = MaterialTheme.colorScheme.surface
    val brush = Brush.linearGradient(
        colors = listOf(base, light, base),
        start = Offset(if (reduceMotion) 0f else shimmer, 0f),
        end = Offset((if (reduceMotion) 0f else shimmer) + SHIMMER_SPAN, 0f),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
        Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(22.dp)).background(brush))
        listOf(1f, 0.86f, 0.64f).forEach { width ->
            Box(Modifier.fillMaxWidth(width).height(16.dp).clip(RoundedCornerShape(8.dp)).background(brush))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sparkle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .size(22.dp)
                    .scale(if (reduceMotion) 1f else pulse),
            )
            Text(
                text = stringResource(R.string.explanation_loading),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

private const val SHIMMER_SPAN = 640f
private const val SHIMMER_MILLIS = 1_200
private const val PULSE_MILLIS = 600

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
