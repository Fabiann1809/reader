package io.github.fabiann1809.reader.ui.quiz

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.TonalButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** The quiz result (T15.4): score, strong and weak topics, and the cards of what was missed. */
@Composable
fun QuizResultView(finished: QuizUiState.Finished, onCreateCards: () -> Unit, onClose: () -> Unit) {
    val result = finished.result
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.quiz_finished_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.quiz_result_score, result.correct, result.total),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.quiz_result_correct_label), style = MaterialTheme.typography.bodyMedium)
        TopicGroup(R.string.quiz_result_strong, result.strongTopics, ReaderTheme.colors.success)
        TopicGroup(R.string.quiz_result_weak, result.weakTopics, ReaderTheme.colors.warning)
        if (result.missed.isNotEmpty()) {
            val count = result.missed.size
            TonalButton(
                text = if (finished.cardsCreated) {
                    pluralStringResource(R.plurals.quiz_result_cards_created, count, count)
                } else {
                    stringResource(R.string.quiz_result_create_cards)
                },
                onClick = onCreateCards,
                enabled = !finished.cardsCreated,
                icon = R.drawable.ic_cards_three,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PrimaryButton(text = stringResource(R.string.quiz_close), onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicGroup(@StringRes title: Int, topics: List<String>, color: Color) {
    if (topics.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = color)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { topic ->
                AssistChip(
                    onClick = {},
                    label = { Text(topic) },
                    shape = MaterialTheme.shapes.small,
                    colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.14f)),
                    border = null,
                )
            }
        }
    }
}
