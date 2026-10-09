package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.QUIZ_SIZES
import io.github.fabiann1809.reader.ui.components.AiButton

/**
 * The reader's ⋮ menu. For now it holds "Ponme a prueba" (T15.3): a quiz about the open chapter, whose
 * number of questions is chosen on the quiz's own first screen. PDFs have no chapter text to read, so it is explained instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenuSheet(isPdf: Boolean, chapterUnreadable: Boolean, onQuiz: (count: Int) -> Unit, onDismiss: () -> Unit) {
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
            if (chapterUnreadable) {
                Text(
                    stringResource(R.string.reader_quiz_unreadable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AiButton(
                text = stringResource(R.string.reader_quiz_start),
                onClick = { onQuiz(QUIZ_SIZES[1]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
