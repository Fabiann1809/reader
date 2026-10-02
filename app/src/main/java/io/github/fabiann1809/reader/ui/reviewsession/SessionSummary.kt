package io.github.fabiann1809.reader.ui.reviewsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.util.daysFromToday
import io.github.fabiann1809.reader.util.formatDate
import java.text.DateFormat
import java.util.Date

/** The end of a session (T14.5): cards reviewed, right answers and when the next card is due. */
@Composable
fun SessionSummary(
    finished: ReviewSessionUiState.Finished,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.padding(24.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = stringResource(R.string.review_summary_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SummaryRow(R.string.review_summary_reviewed, finished.reviewed.toString())
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SummaryRow(
                    R.string.review_summary_correct,
                    stringResource(R.string.review_summary_correct_value, finished.correct, finished.reviewed),
                )
                finished.nextReviewAt?.let { next ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryRow(R.string.review_summary_next, nextReviewLabel(next, now))
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.review_summary_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryRow(label: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 14.dp)) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
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
