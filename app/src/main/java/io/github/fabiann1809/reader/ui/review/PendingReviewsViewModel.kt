package io.github.fabiann1809.reader.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.util.endOfDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Whether there are cards to review today: the dot on the Repasar tab (design 03 §1). */
class PendingReviewsViewModel(
    flashcardRepository: FlashcardRepository,
    now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    // "Today" is read again whenever the dot is watched anew (e.g. back in the app the next day).
    val hasPending: StateFlow<Boolean> = flow { emitAll(flashcardRepository.observeDue(endOfDay(now()))) }
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
