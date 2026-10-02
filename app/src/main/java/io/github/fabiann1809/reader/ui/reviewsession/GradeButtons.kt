package io.github.fabiann1809.reader.ui.reviewsession

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.data.flashcard.outcomeOf
import kotlin.time.Duration

/** Otra vez · Difícil · Bien · Fácil, each with when the card would come back (lámina 1h). */
@Composable
fun GradeButtons(card: Flashcard, onGrade: (ReviewGrade) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        ReviewGrade.entries.forEach { grade ->
            Surface(
                onClick = { onGrade(grade) },
                shape = MaterialTheme.shapes.medium,
                color = if (grade == ReviewGrade.AGAIN) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    Text(stringResource(grade.label()), style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = waitLabel(card.outcomeOf(grade).wait),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
