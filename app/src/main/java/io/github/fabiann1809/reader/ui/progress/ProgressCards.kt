package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.GoalUnit
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import java.time.format.TextStyle
import java.util.Locale

/** A card of the Progreso tab: paper-200, 16 dp corners (lámina 1i). */
@Composable
private fun ProgressCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

/**
 * The daily goal's ring ("24 de 30 min") and the streak's flame ("6 días seguidos leyendo"). Tapping
 * it changes the goal ([onEditGoal], T16.3).
 */
@Composable
fun GoalCard(stats: ProgressStats, onEditGoal: () -> Unit = {}) {
    ProgressCard(onClick = onEditGoal) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GoalRing(stats)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_flame_fill),
                        contentDescription = null,
                        tint = ReaderTheme.colors.warning,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(stats.streakDays.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    pluralStringResource(R.plurals.progress_streak_days, stats.streakDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GoalRing(stats: ProgressStats) {
    val track = MaterialTheme.colorScheme.outline
    val progress = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            if (stats.goalProgress > 0f) {
                drawArc(
                    progress,
                    startAngle = -90f,
                    sweepAngle = 360f * stats.goalProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stats.todayAmount.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(
                    if (stats.goal.unit == GoalUnit.PAGES) R.string.progress_goal_of_pages else R.string.progress_goal_of,
                    stats.goal.amount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Esta semana": a bar per day and "3 h 48 m esta semana · 42 min de media". */
@Composable
fun WeekCard(stats: ProgressStats) {
    // A goal in minutes sets the scale, so a day that met it fills the bar; with a goal in pages,
    // half an hour does, so a few minutes don't look like a full day.
    val goalMinutes = if (stats.goal.unit == GoalUnit.MINUTES) stats.goal.amount else ReadingGoal.DEFAULT_GOAL_MINUTES
    val scale = maxOf(goalMinutes, stats.week.maxOf { it.minutes }, 1)
    ProgressCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.progress_this_week), style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(96.dp),
            ) {
                stats.week.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(day.minutes.toFloat() / scale)
                                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                            )
                        }
                        Text(
                            day.day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.progress_week_summary, durationText(stats.weekMinutes), stats.weekAverageMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Libros terminados · En 2026 · 8". */
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
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                stats.finishedThisYear.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** "3 h 48 m", or "42 min" under an hour. */
@Composable
private fun durationText(minutes: Int): String =
    if (minutes >= 60) {
        stringResource(R.string.progress_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.progress_minutes, minutes)
    }
