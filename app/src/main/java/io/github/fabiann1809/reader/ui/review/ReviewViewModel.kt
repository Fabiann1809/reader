package io.github.fabiann1809.reader.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.util.endOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** The Repasar tab (T14.3): the cards due today, shown all together, by book or by label. */
class ReviewViewModel(
    flashcardRepository: FlashcardRepository,
    bookRepository: BookRepository,
    now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val mode = MutableStateFlow(ReviewMode.TODAY)

    // "Today" is read again whenever the tab is watched anew, so a new day brings its cards.
    private val dueCards = flow { emitAll(flashcardRepository.observeDue(endOfDay(now()))) }

    val uiState: StateFlow<ReviewUiState> =
        combine(dueCards, bookRepository.observeBooks(), mode) { cards, books, mode ->
            val titles = books.associate { it.id to it.title }
            ReviewUiState(
                isLoading = false,
                dueCards = cards.map { DueCard(it, titles[it.bookId].orEmpty()) },
                mode = mode,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReviewUiState())

    fun selectMode(mode: ReviewMode) {
        this.mode.value = mode
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
