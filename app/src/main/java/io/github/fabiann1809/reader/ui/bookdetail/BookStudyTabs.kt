package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.formatDate

/** "Resumen": time read, sessions, notes and cards, and the AI block that only works on request. */
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
    item(key = "summary_ai") { AiCard(state, onReviewBook, onQuizBook) }
}

@Composable
private fun AiCard(state: BookDetailUiState.Success, onReviewBook: () -> Unit, onQuizBook: () -> Unit) {
    val ai = ReaderTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(ai.aiContainer, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_sparkle), contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.detail_ai_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiPill(
                text = stringResource(R.string.detail_review_short),
                filled = true,
                enabled = state.dueCards > 0,
                onClick = onReviewBook,
                modifier = Modifier.weight(1f),
            )
            AiPill(
                text = stringResource(R.string.quiz_try_me),
                filled = false,
                enabled = state.quizSource.isNotBlank(),
                onClick = onQuizBook,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.quizSource.isBlank()) {
            Text(
                stringResource(R.string.detail_quiz_needs_notes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiPill(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.tertiary
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(color) else Modifier.border(1.5.dp, color, CircleShape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** "Fichas": how many there are and how many are due, then each card with when it comes back. */
fun LazyListScope.cardsTab(cards: List<Flashcard>, dueCards: Int, onAddFlashcard: () -> Unit, onReviewBook: () -> Unit) {
    if (cards.isNotEmpty()) {
        item(key = "cards_summary") { CardsSummary(cards.size, dueCards, onReviewBook) }
    }
    item(key = "cards_new") { AddLink(R.string.flashcard_new, onAddFlashcard) }
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

@Composable
private fun CardsSummary(count: Int, due: Int, onReview: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cards_three),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(pluralStringResource(R.plurals.detail_cards_count, count, count), style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(R.plurals.detail_cards_due, due, due),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .alpha(if (due > 0) 1f else 0.4f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.inverseSurface)
                .clickable(enabled = due > 0, role = Role.Button, onClick = onReview)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                stringResource(R.string.detail_cards_review),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

/** "Sesiones": when, how many pages and for how long, newest first. */
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
    items(sessions, key = { "session_${it.id}" }) { session -> SessionRow(session) }
}

@Composable
private fun SessionRow(session: ReadingSession) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(min = 60.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_timer),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(formatDate(session.startedAt), style = MaterialTheme.typography.titleSmall)
                Text(
                    pluralStringResource(R.plurals.detail_session_pages, session.pagesRead, session.pagesRead),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // A paper book's session only knows its pages.
            if (session.durationMillis > 0) {
                val minutes = (session.durationMillis / MILLIS_PER_MINUTE).toInt()
                Text(
                    text = if (minutes == 0) stringResource(R.string.detail_session_short) else durationText(minutes),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(22.dp))
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
