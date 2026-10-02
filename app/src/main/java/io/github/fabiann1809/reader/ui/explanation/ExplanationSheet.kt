package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R

/**
 * The explainer as a bottom sheet over the reader (design 10.5, T11.11): "Explicación" with the
 * AI label, then the same body as the explainer screen. Closing it leaves the page as it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationSheet(
    uiState: ExplanationUiState,
    onRetry: () -> Unit,
    onSaveAsNote: () -> Unit,
    onDismiss: () -> Unit,
    onCreateFlashcard: (() -> Unit)? = null,
) {
    // Expanded right away: the explanation is long, and a half sheet would hide most of it.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.explanation_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                AiGeneratedChip()
            }
            ExplanationBody(uiState = uiState, onRetry = onRetry, onSaveAsNote = onSaveAsNote, onCreateFlashcard = onCreateFlashcard)
        }
    }
}
