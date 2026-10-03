package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.QUIZ_SIZES
import io.github.fabiann1809.reader.ui.components.AiButton

/**
 * The reader's ⋮ menu. For now it holds "Ponme a prueba" (T15.3): a quiz of 3, 5 or 10 questions
 * about the open chapter. PDFs have no chapter text to read, so it is explained instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuSheet(isPdf: Boolean, chapterUnreadable: Boolean, onQuiz: (count: Int) -> Unit, onDismiss: () -> Unit) {
    var count by rememberSaveable { mutableIntStateOf(QUIZ_SIZES[1]) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
        ) {
            Text(stringResource(R.string.quiz_try_me), style = MaterialTheme.typography.titleLarge)
            if (isPdf) {
                Text(stringResource(R.string.reader_quiz_pdf), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text(stringResource(R.string.reader_quiz_about_chapter), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUIZ_SIZES.forEach { size ->
                    FilterChip(
                        selected = size == count,
                        onClick = { count = size },
                        label = { Text(stringResource(R.string.reader_quiz_questions, size)) },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }
            if (chapterUnreadable) {
                Text(
                    stringResource(R.string.reader_quiz_unreadable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AiButton(
                text = stringResource(R.string.reader_quiz_start),
                onClick = { onQuiz(count) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
