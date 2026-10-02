package io.github.fabiann1809.reader.ui.reviewsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar

/** A review session (T14.4); at the end its summary (T14.5), whose "Listo" closes it ([onFinished]). */
@Composable
fun ReviewSessionScreen(
    onNavigateUp: () -> Unit,
    onFinished: () -> Unit,
    viewModel: ReviewSessionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Nothing was due (e.g. reviewed elsewhere meanwhile): there is no session to sum up.
    val nothingReviewed = (uiState as? ReviewSessionUiState.Finished)?.reviewed == 0
    LaunchedEffect(nothingReviewed) {
        if (nothingReviewed) onFinished()
    }
    ReviewSessionContent(
        uiState,
        onFlip = viewModel::flip,
        onGrade = viewModel::grade,
        onNavigateUp = onNavigateUp,
        onDone = onFinished,
    )
}

@Composable
fun ReviewSessionContent(
    uiState: ReviewSessionUiState,
    onFlip: () -> Unit,
    onGrade: (ReviewGrade) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val reviewing = uiState as? ReviewSessionUiState.Reviewing
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(
                // "7 / 20": which card of the session this is.
                title = reviewing?.let { stringResource(R.string.review_session_counter, it.index + 1, it.cards.size) }.orEmpty(),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        val finished = uiState as? ReviewSessionUiState.Finished
        if (finished != null && finished.reviewed > 0) {
            SessionSummary(
                finished,
                onDone = onDone,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }
        if (reviewing == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            FlipCard(
                dueCard = reviewing.current,
                flipped = reviewing.flipped,
                onFlip = onFlip,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            // Same height either way, so the card doesn't jump when the buttons appear.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                if (reviewing.flipped) {
                    GradeButtons(reviewing.current.card, onGrade)
                } else {
                    Text(
                        text = stringResource(R.string.review_tap_to_flip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
