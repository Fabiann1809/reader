package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The explainer as a bottom sheet over the reader (T11.11): "Explicación" with the AI label, the
 * same body as the explainer screen and its actions pinned below. Closing it leaves the page as it was.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationSheet(
    uiState: ExplanationUiState,
    onRetry: () -> Unit,
    onSaveAsNote: () -> Unit,
    onDismiss: () -> Unit,
    onCreateFlashcard: (() -> Unit)? = null,
    onNowYou: (() -> Unit)? = null,
    onQuiz: (() -> Unit)? = null,
) {
    // Expanded right away: the explanation is long, and a half sheet would hide most of it.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT)
                .navigationBarsPadding(),
        ) {
            ExplanationHeader(onClose = onDismiss)
            ExplanationBody(
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )
            ExplanationActions(uiState, onSaveAsNote, onCreateFlashcard, onNowYou, onQuiz)
        }
    }
}

private const val SHEET_HEIGHT = 0.86f
