package io.github.fabiann1809.reader.ui.review

import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.note.NoteTag

/** The chips of the Repasar tab (lámina 1h): every card due today, or grouped by book or by label. */
enum class ReviewMode { TODAY, BY_BOOK, BY_TAG }

/** A card due today with the title of its book, to show where it comes from. */
data class DueCard(val card: Flashcard, val bookTitle: String)

/** A heading and its cards; [tag] is set when grouped by label (null for "Sin etiqueta"). */
data class CardGroup(val title: String?, val tag: NoteTag?, val cards: List<DueCard>)

data class ReviewUiState(
    val isLoading: Boolean = true,
    val dueCards: List<DueCard> = emptyList(),
    val mode: ReviewMode = ReviewMode.TODAY,
) {
    /** The cards as the chips show them: one group for "Hoy", one per book or one per label. */
    val groups: List<CardGroup>
        get() = when (mode) {
            ReviewMode.TODAY -> listOf(CardGroup(title = null, tag = null, cards = dueCards)).filter { it.cards.isNotEmpty() }
            ReviewMode.BY_BOOK -> dueCards.groupBy { it.card.bookId }.values
                .map { cards -> CardGroup(title = cards.first().bookTitle, tag = null, cards = cards) }
                .sortedBy { it.title }
            ReviewMode.BY_TAG -> dueCards.groupBy { it.card.tag }.entries
                // The labels in their usual order (Idea, Duda, Cita, Tarea), cards without one last.
                .sortedBy { (tag, _) -> tag?.ordinal ?: Int.MAX_VALUE }
                .map { (tag, cards) -> CardGroup(title = null, tag = tag, cards = cards) }
        }
}
