package io.github.fabiann1809.reader.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.explanation.ExplanationSheet
import io.github.fabiann1809.reader.ui.explanation.ExplanationViewModel

/**
 * Explains [text] selected in book [bookId] without leaving the reader (T11.11). The ViewModel is
 * keyed by the text, so reopening the same selection shows the answer again without asking the AI.
 */
@Composable
fun ReaderExplanation(bookId: Long, text: String, onDismiss: () -> Unit) {
    val viewModel: ExplanationViewModel = viewModel(
        key = "explanation:$bookId:$text",
        factory = AppViewModelProvider.explanationInReader(bookId, text),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExplanationSheet(
        uiState = uiState,
        onRetry = viewModel::retry,
        onSaveAsNote = viewModel::saveAsNote,
        onDismiss = onDismiss,
    )
}
