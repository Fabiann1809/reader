package io.github.fabiann1809.reader.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.data.prefs.ReadingGoalStore
import io.github.fabiann1809.reader.data.session.ReadingSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Null [stats] while loading; [isEmpty] when nothing was read yet ("Empieza tu primera sesión"). */
data class ProgressUiState(val stats: ProgressStats? = null, val isEmpty: Boolean = false)

/** The Progreso tab (T16.2): today's goal, the streak, this week and the books finished this year. */
class ProgressViewModel(
    sessionRepository: ReadingSessionRepository,
    bookRepository: BookRepository,
    private val goalStore: ReadingGoalStore,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    // "Now" is read again whenever the tab is watched anew, so a new day shows its own numbers.
    private val sessions = flow { emitAll(sessionRepository.observeSince(now() - STREAK_LOOKBACK_MILLIS)) }

    val uiState: StateFlow<ProgressUiState> =
        combine(sessions, bookRepository.observeBooks(), goalStore.goal) { sessions, books, goal ->
            val stats = progressStats(sessions, books, goal, now())
            ProgressUiState(stats, isEmpty = sessions.isEmpty() && stats.finishedThisYear == 0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgressUiState())

    /** The goal chosen in the goal dialog (T16.3). */
    fun setGoal(goal: ReadingGoal) {
        viewModelScope.launch { goalStore.setGoal(goal) }
    }

    private companion object {
        // A year of sessions is enough for any realistic streak and the weekly chart.
        val STREAK_LOOKBACK_MILLIS = TimeUnit.DAYS.toMillis(366)
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
