package io.github.fabiann1809.reader.ui.quiz

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.TonalButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** The quiz result: score ring, strong and weak topics, and the cards of what was missed. */
@Composable
fun QuizResultView(finished: QuizUiState.Finished, onCreateCards: () -> Unit, onRepeat: () -> Unit, onClose: () -> Unit) {
    val result = finished.result
    val colors = ReaderTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        ScoreRing(result.correct, result.total)
        Text(stringResource(R.string.quiz_finished_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.quiz_result_correct_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TopicGroup(R.string.quiz_result_strong, result.strongTopics, colors.success, colors.successContainer, colors.onSuccessContainer)
        TopicGroup(
            R.string.quiz_result_weak,
            result.weakTopics,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlineButton(text = stringResource(R.string.quiz_repeat), onClick = onRepeat, modifier = Modifier.weight(1f))
            PrimaryButton(text = stringResource(R.string.quiz_close), onClick = onClose, modifier = Modifier.weight(1f))
        }
    }
}

/** A 150 dp ring filled to correct/total, with "3/5" in the middle. */
@Composable
private fun ScoreRing(correct: Int, total: Int) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = ReaderTheme.colors.success
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
            if (total > 0) drawArc(fill, -90f, 360f * correct / total, false, Offset(inset, inset), arcSize, style = stroke)
        }
        Text(
            text = stringResource(R.string.quiz_result_score, correct, total),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicGroup(@StringRes title: Int, topics: List<String>, accent: Color, container: Color, ink: Color) {
    if (topics.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = accent)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { topic ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(34.dp)
                        .background(container, CircleShape)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(topic, style = MaterialTheme.typography.labelLarge, color = ink)
                }
            }
        }
    }
}
