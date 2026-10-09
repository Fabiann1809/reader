package io.github.fabiann1809.reader.ui.reviewsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.SessionHeader

/** A review session (T14.4); at the end its summary (T14.5), whose "Listo" closes it ([onFinished]). */
@Composable
fun ReviewSessionScreen(
    onNavigateUp: () -> Unit,
    onFinished: () -> Unit,
    // "Ponme a prueba" in the summary: a quiz about the reviewed cards.
    onQuiz: (bookId: Long, source: String, count: Int, title: String) -> Unit = { _, _, _, _ -> },
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
        onQuiz = onQuiz,
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
    onQuiz: (bookId: Long, source: String, count: Int, title: String) -> Unit = { _, _, _, _ -> },
) {
    val reviewing = uiState as? ReviewSessionUiState.Reviewing
    val finished = (uiState as? ReviewSessionUiState.Finished)?.takeIf { it.reviewed > 0 }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
            SessionHeader(
                // "7 / 20": which card of the session this is.
                counter = reviewing?.let { stringResource(R.string.review_session_counter, it.index + 1, it.cards.size) },
                progress = reviewing?.let { it.index.toFloat() / it.cards.size },
                onClose = onNavigateUp,
            )
            when {
                finished != null -> FinishedBody(finished, onDone, onQuiz)
                reviewing == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> ReviewingBody(reviewing, onFlip, onGrade)
            }
        }
    }
}

@Composable
private fun FinishedBody(finished: ReviewSessionUiState.Finished, onDone: () -> Unit, onQuiz: (Long, String, Int, String) -> Unit) {
    val reviewTitle = stringResource(R.string.quiz_title_review)
    SessionSummary(
        finished,
        onDone = onDone,
        // A few cards make a short quiz; many, a longer one.
        onQuiz = { onQuiz(finished.quizBookId, finished.quizSource, if (finished.reviewed >= 5) 5 else 3, reviewTitle) },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ReviewingBody(reviewing: ReviewSessionUiState.Reviewing, onFlip: () -> Unit, onGrade: (ReviewGrade) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
    ) {
        FlipCard(
            dueCard = reviewing.current,
            flipped = reviewing.flipped,
            onFlip = onFlip,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        // Same height either way, so the card doesn't jump when the buttons change.
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            if (reviewing.flipped) {
                GradeButtons(reviewing.current.card, onGrade)
            } else {
                PrimaryButton(text = stringResource(R.string.review_show_answer), onClick = onFlip, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
