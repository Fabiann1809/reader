package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R

private val CardShape = RoundedCornerShape(30.dp)

/** A card of the Progreso tab: surface-container, 30 dp corners. */
@Composable
fun ProgressCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) { content() }
}

/** "Libros terminados · En 2026" and how many, big, in the accent color. */
@Composable
fun FinishedBooksCard(stats: ProgressStats) {
    ProgressCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.progress_finished_books), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.progress_in_year, stats.year),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                stats.finishedThisYear.toString(),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** "3 h 48 m", or "42 min" under an hour. */
@Composable
fun durationText(minutes: Int): String =
    if (minutes >= 60) {
        stringResource(R.string.progress_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.progress_minutes, minutes)
    }
