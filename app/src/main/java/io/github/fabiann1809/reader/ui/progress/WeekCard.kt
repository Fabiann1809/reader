package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.GoalUnit
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** "Esta semana": the total, a bar per day (today's in the accent color) and the daily average. */
@Composable
fun WeekCard(stats: ProgressStats, today: LocalDate = LocalDate.now()) {
    // A goal in minutes sets the scale, so a day that met it fills the bar; with a goal in pages,
    // half an hour does, so a few minutes don't look like a full day.
    val goalMinutes = if (stats.goal.unit == GoalUnit.MINUTES) stats.goal.amount else ReadingGoal.DEFAULT_GOAL_MINUTES
    val scale = maxOf(goalMinutes, stats.week.maxOf { it.minutes }, 1)
    ProgressCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.progress_this_week),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    durationText(stats.weekMinutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(110.dp)) {
                stats.week.forEach { day ->
                    DayBar(day, scale, isToday = day.day == today.dayOfWeek, modifier = Modifier.weight(1f))
                }
            }
            if (stats.weekAverageMinutes > 0) {
                Text(
                    stringResource(R.string.progress_week_average, stats.weekAverageMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayBar(day: DayMinutes, scale: Int, isToday: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        modifier = modifier.fillMaxHeight(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(maxOf(day.minutes.toFloat() / scale, MIN_BAR))
                    .background(
                        if (isToday) scheme.primary else scheme.primaryContainer,
                        RoundedCornerShape(10.dp),
                    ),
            )
        }
        Text(
            day.day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = if (isToday) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

// Days without reading still show a small stub, so the week reads as seven bars.
private const val MIN_BAR = 0.04f
