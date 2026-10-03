package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.ui.components.TonalButton
import io.github.fabiann1809.reader.util.formatDate

/** "Resumen" (T16.4): time read, sessions, notes and cards, and the optional AI block. */
fun LazyListScope.summaryTab(state: BookDetailUiState.Success, onReviewBook: () -> Unit, onQuizBook: () -> Unit) {
    item(key = "summary_facts") {
        DetailCard {
            val minutes = (state.sessions.sumOf { it.durationMillis } / MILLIS_PER_MINUTE).toInt()
            Fact(R.string.detail_fact_time, durationText(minutes))
            Fact(R.string.detail_fact_sessions, state.sessions.size.toString())
            Fact(R.string.detail_fact_notes, state.notes.size.toString())
            Fact(R.string.detail_fact_cards, state.flashcards.size.toString())
        }
    }
    // Design 01 §4.2: "Con IA, si quieres" — only on request, never automatic.
    item(key = "summary_ai") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.detail_ai_title), style = MaterialTheme.typography.titleMedium)
            TonalButton(
                text = pluralStringResource(R.plurals.detail_review_book, state.dueCards, state.dueCards),
                onClick = onReviewBook,
                enabled = state.dueCards > 0,
                icon = R.drawable.ic_cards,
                modifier = Modifier.fillMaxWidth(),
            )
            AiButton(
                text = stringResource(R.string.quiz_try_me),
                onClick = onQuizBook,
                enabled = state.quizSource.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.quizSource.isBlank()) {
                Text(
                    stringResource(R.string.detail_quiz_needs_notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Fichas" (T16.4): the book's cards, with when each comes back. */
fun LazyListScope.cardsTab(cards: List<Flashcard>) {
    if (cards.isEmpty()) {
        item(key = "cards_empty") {
            StatusMessage(
                icon = R.drawable.ic_cards,
                title = stringResource(R.string.detail_cards_empty),
                message = stringResource(R.string.detail_cards_empty_message),
            )
        }
        return
    }
    items(cards, key = { "card_${it.id}" }) { card ->
        DetailCard {
            Text(card.front, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                card.back,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.detail_card_next, formatDate(card.nextReviewAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** "Sesiones" (T16.4): when, for how long and how many pages, newest first. */
fun LazyListScope.sessionsTab(sessions: List<ReadingSession>) {
    if (sessions.isEmpty()) {
        item(key = "sessions_empty") {
            StatusMessage(
                icon = R.drawable.ic_chart_donut,
                title = stringResource(R.string.detail_sessions_empty),
                message = stringResource(R.string.detail_sessions_empty_message),
            )
        }
        return
    }
    items(sessions, key = { "session_${it.id}" }) { session ->
        DetailCard {
            Row {
                Text(formatDate(session.startedAt), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    pluralStringResource(R.plurals.detail_session_pages, session.pagesRead, session.pagesRead),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val minutes = (session.durationMillis / MILLIS_PER_MINUTE).toInt()
            // A paper book's session only knows its pages.
            if (session.durationMillis > 0) {
                val duration = if (minutes == 0) stringResource(R.string.detail_session_short) else durationText(minutes)
                Text(duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun Fact(label: Int, value: String) {
    Row {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

/** "3 h 20 m", or "42 min" under an hour. */
@Composable
fun durationText(minutes: Int): String =
    if (minutes >= 60) {
        stringResource(R.string.progress_hours_minutes, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.progress_minutes, minutes)
    }

private const val MILLIS_PER_MINUTE = 60_000L
