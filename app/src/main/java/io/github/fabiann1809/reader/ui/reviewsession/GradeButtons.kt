package io.github.fabiann1809.reader.ui.reviewsession

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.data.flashcard.outcomeOf
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.time.Duration

/** Otra vez · Difícil · Bien · Fácil, each with when the card would come back. */
@Composable
fun GradeButtons(card: Flashcard, onGrade: (ReviewGrade) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        ReviewGrade.entries.forEach { grade ->
            val (container, ink) = grade.colors()
            Surface(
                onClick = { onGrade(grade) },
                shape = RoundedCornerShape(20.dp),
                color = container,
                contentColor = ink,
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.height(64.dp),
                ) {
                    Text(stringResource(grade.label()), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                    Text(
                        text = waitLabel(card.outcomeOf(grade).wait),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.alpha(0.8f),
                    )
                }
            }
        }
    }
}

/** Warm to cool: again (error), hard (warning), good (success), easy (accent). */
@Composable
private fun ReviewGrade.colors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val extra = ReaderTheme.colors
    return when (this) {
        ReviewGrade.AGAIN -> scheme.errorContainer to scheme.onErrorContainer
        ReviewGrade.HARD -> extra.warningContainer to extra.onWarningContainer
        ReviewGrade.GOOD -> extra.successContainer to extra.onSuccessContainer
        ReviewGrade.EASY -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
}

/** "1 min", "10 min", "3 días": how long until the card comes back. */
@Composable
private fun waitLabel(wait: Duration): String {
    val days = wait.inWholeDays.toInt()
    return if (days >= 1) {
        pluralStringResource(R.plurals.review_wait_days, days, days)
    } else {
        stringResource(R.string.review_wait_minutes, wait.inWholeMinutes.toInt())
    }
}

@StringRes
private fun ReviewGrade.label(): Int = when (this) {
    ReviewGrade.AGAIN -> R.string.review_grade_again
    ReviewGrade.HARD -> R.string.review_grade_hard
    ReviewGrade.GOOD -> R.string.review_grade_good
    ReviewGrade.EASY -> R.string.review_grade_easy
}
