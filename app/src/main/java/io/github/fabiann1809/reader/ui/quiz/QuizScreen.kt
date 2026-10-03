package io.github.fabiann1809.reader.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
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
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding(),
    ) {
        val answering = uiState as? QuizUiState.Answering
        QuizTopBar(title, answering, onLeave)
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
                is QuizUiState.Finished -> Centered {
                    Text(stringResource(R.string.quiz_finished_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.quiz_finished_score, uiState.correct, uiState.total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    PrimaryButton(text = stringResource(R.string.quiz_close), onClick = onClose)
                }
            }
        }
    }
}

/** ✕, "Cap. 4 · Pregunta 2/5" and the thin progress bar. */
@Composable
private fun QuizTopBar(title: String, answering: QuizUiState.Answering?, onLeave: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(56.dp)) {
        IconButton(onClick = onLeave) {
            Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.quiz_leave_title))
        }
        val counter = answering?.let {
            stringResource(R.string.quiz_counter, title, it.index + 1, it.quiz.questions.size)
        } ?: title
        Text(counter, style = MaterialTheme.typography.titleSmall)
    }
    if (answering != null) {
        LinearProgressIndicator(
            progress = { (answering.index + 1f) / answering.quiz.questions.size },
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outline,
            drawStopIndicator = {},
            gapSize = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
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
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Text(question.question, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            question.options.forEachIndexed { index, option ->
                QuizOption(
                    letter = 'A' + index,
                    text = option,
                    look = optionLook(index, state.chosen, question.correctIndex),
                    onClick = { onChoose(index) },
                )
            }
            state.chosen?.let { chosen ->
                val right = chosen == question.correctIndex
                Text(
                    text = stringResource(
                        if (right) R.string.quiz_feedback_correct else R.string.quiz_feedback_wrong,
                        question.explanation,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        .padding(14.dp),
                )
            }
        }
        PrimaryButton(
            text = stringResource(if (state.isLast) R.string.quiz_see_result else R.string.quiz_next),
            onClick = onNext,
            enabled = state.chosen != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        )
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
