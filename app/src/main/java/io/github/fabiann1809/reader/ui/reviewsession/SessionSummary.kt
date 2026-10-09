package io.github.fabiann1809.reader.ui.reviewsession

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.daysFromToday
import io.github.fabiann1809.reader.util.formatDate
import java.text.DateFormat
import java.util.Date

/** The end of a session: cards reviewed, right answers and when the next card is due. */
@Composable
fun SessionSummary(
    finished: ReviewSessionUiState.Finished,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onQuiz: () -> Unit = {},
    now: Long = System.currentTimeMillis(),
) {
    Column(modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 36.dp),
        ) {
            DoneBadge()
            Text(
                text = stringResource(R.string.review_summary_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(finished.reviewed.toString(), stringResource(R.string.review_summary_reviewed), Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.review_summary_correct_value, finished.correct, finished.reviewed),
                    stringResource(R.string.review_summary_correct),
                    Modifier.weight(1f),
                )
                finished.nextReviewAt?.let { next ->
                    StatTile(nextReviewLabel(next, now), stringResource(R.string.review_summary_next), Modifier.weight(1f))
                }
            }
        }
        // Optional, never automatic: "Ponme a prueba" is an option.
        AiButton(
            text = stringResource(R.string.quiz_try_me),
            onClick = onQuiz,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(R.string.review_summary_done),
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun DoneBadge() {
    val colors = ReaderTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(96.dp)
            .background(colors.successContainer, CircleShape),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = colors.success,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 16.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Hoy, 14:35", "Mañana" or a date: when the next card comes back. */
@Composable
private fun nextReviewLabel(next: Long, now: Long): String = when (daysFromToday(next, now)) {
    in Long.MIN_VALUE..0L -> stringResource(
        R.string.review_summary_today_at,
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(maxOf(next, now))),
    )
    1L -> stringResource(R.string.review_summary_tomorrow)
    else -> formatDate(next)
}
