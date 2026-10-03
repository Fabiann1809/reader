package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.delay

// Long enough to read it and decide; then it goes by itself so reading isn't interrupted.
private const val VISIBLE_MILLIS = 12_000L

/**
 * The end-of-chapter suggestion (T15.5, design 02: discreet and dismissible, never modal): a small
 * card over the page with "Repasar" and "Ponme a prueba". It hides by itself after a while.
 */
@Composable
fun ChapterEndSuggestion(
    end: ChapterEnd,
    onReview: () -> Unit,
    onQuiz: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(end) {
        delay(VISIBLE_MILLIS)
        dismiss()
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = ReaderTheme.colors.aiSoft,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.chapter_end_question, end.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painterResource(R.drawable.ic_x),
                        contentDescription = stringResource(R.string.chapter_end_dismiss),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onReview) { Text(stringResource(R.string.chapter_end_review)) }
                TextButton(onClick = onQuiz) { Text(stringResource(R.string.quiz_try_me), color = ReaderTheme.colors.ai) }
            }
        }
    }
}
