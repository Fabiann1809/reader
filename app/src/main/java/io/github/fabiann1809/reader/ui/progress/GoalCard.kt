package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.GoalUnit
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** The daily goal's ring ("24 de 30 min") and what is left to meet it. Tapping it changes the goal. */
@Composable
fun GoalCard(stats: ProgressStats, onEditGoal: () -> Unit = {}) {
    ProgressCard(onClick = onEditGoal) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GoalRing(stats)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.goal_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(goalMessage(stats), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    stringResource(R.string.progress_goal_edit_hint),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun goalMessage(stats: ProgressStats): String {
    val left = stats.goal.amount - stats.todayAmount
    return when {
        left <= 0 -> stringResource(R.string.progress_goal_done)
        stats.goal.unit == GoalUnit.PAGES -> pluralStringResource(R.plurals.progress_goal_left_pages, left, left)
        else -> stringResource(R.string.progress_goal_left_minutes, left)
    }
}

@Composable
private fun GoalRing(stats: ProgressStats) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val progress = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = stroke)
            if (stats.goalProgress > 0f) {
                drawArc(progress, -90f, 360f * stats.goalProgress, false, topLeft, arcSize, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stats.todayAmount.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold),
            )
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

/** "6 días seguidos" on a pastel card, with the flame, which flares when [celebrate]. */
@Composable
fun StreakCard(stats: ProgressStats, celebrate: Boolean = false, onCelebrated: () -> Unit = {}) {
    val colors = ReaderTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(colors.pastels[3]))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.7f), CircleShape),
        ) {
            StreakFlame(celebrate = celebrate, onCelebrated = onCelebrated)
        }
        Column {
            Text(
                pluralStringResource(R.plurals.progress_streak_title, stats.streakDays, stats.streakDays),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.onPastel,
            )
            Text(
                stringResource(R.string.progress_streak_hint),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPastel,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
