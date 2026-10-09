package io.github.fabiann1809.reader.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.SessionHeader
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** A quiz (lámina 1h) titled [title] (e.g. the chapter); leaving it halfway asks first. */
@Composable
fun QuizScreen(
    title: String,
    onClose: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var askingToLeave by rememberSaveable { mutableStateOf(false) }
    val answering = uiState is QuizUiState.Answering
    val leave = { if (answering) askingToLeave = true else onClose() }
    BackHandler(onBack = leave)

    QuizContent(
        title = title,
        uiState = uiState,
        onChoose = viewModel::choose,
        onNext = viewModel::next,
        onRetry = viewModel::retry,
        onLeave = leave,
        onClose = onClose,
        onCreateCards = viewModel::createCardsFromMistakes,
    )
    if (askingToLeave) {
        AlertDialog(
            onDismissRequest = { askingToLeave = false },
            title = { Text(stringResource(R.string.quiz_leave_title)) },
            text = { Text(stringResource(R.string.quiz_leave_message)) },
            confirmButton = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.quiz_leave_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { askingToLeave = false }) { Text(stringResource(R.string.quiz_leave_stay)) }
            },
        )
    }
}

@Composable
fun QuizContent(
    title: String,
    uiState: QuizUiState,
    onChoose: (Int) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onLeave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateCards: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        val answering = uiState as? QuizUiState.Answering
        SessionHeader(
            counter = answering?.let { stringResource(R.string.quiz_counter, title, it.index + 1, it.quiz.questions.size) } ?: title,
            progress = answering?.let { (it.index + 1f) / it.quiz.questions.size },
            onClose = onLeave,
        )
        Box(Modifier.weight(1f)) {
            when (uiState) {
                QuizUiState.Loading -> Centered {
                    CircularProgressIndicator(color = ReaderTheme.colors.ai)
                    Text(stringResource(R.string.quiz_loading), style = MaterialTheme.typography.bodyMedium)
                }
                is QuizUiState.Failed -> Centered {
                    Text(
                        stringResource(aiErrorMessageRes(uiState.error)),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    PrimaryButton(text = stringResource(R.string.quiz_retry), onClick = onRetry)
                }
                is QuizUiState.Answering -> QuestionPage(uiState, onChoose, onNext)
                is QuizUiState.Finished -> QuizResultView(uiState, onCreateCards = onCreateCards, onClose = onClose)
            }
        }
    }
}

@Composable
private fun QuestionPage(state: QuizUiState.Answering, onChoose: (Int) -> Unit, onNext: () -> Unit) {
    val question = state.question
    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Text(
                question.question,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            question.options.forEachIndexed { index, option ->
                QuizOption(
                    letter = 'A' + index,
                    text = option,
                    look = optionLook(index, state.chosen, question.correctIndex),
                    onClick = { onChoose(index) },
                )
            }
            state.chosen?.let { chosen -> Feedback(right = chosen == question.correctIndex, explanation = question.explanation) }
        }
        PrimaryButton(
            text = stringResource(if (state.isLast) R.string.quiz_see_result else R.string.quiz_next),
            onClick = onNext,
            enabled = state.chosen != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        )
    }
}

/** "¡Correcto!" or "No exactamente", then why. */
@Composable
private fun Feedback(right: Boolean, explanation: String) {
    val colors = ReaderTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(if (right) R.string.quiz_feedback_correct else R.string.quiz_feedback_wrong),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = if (right) colors.success else MaterialTheme.colorScheme.error,
        )
        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) { content() }
}
