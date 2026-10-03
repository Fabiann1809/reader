package io.github.fabiann1809.reader.ui.progress

import io.github.fabiann1809.reader.data.prefs.GoalUnit
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.data.prefs.ReadingGoalStore
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeReadingSessionRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeGoalStore : ReadingGoalStore {
        override val goal = MutableStateFlow(ReadingGoal())

        override suspend fun setGoal(goal: ReadingGoal) {
            this.goal.value = goal
        }
    }

    private val now = 1_790_900_000_000L
    private val goalStore = FakeGoalStore()

    @Test
    fun theRingFollowsTheChosenGoal() = runTest(mainDispatcherRule.testDispatcher) {
        val sessions = FakeReadingSessionRepository(
            listOf(ReadingSession(id = 1, bookId = 1, startedAt = now - 600_000, endedAt = now, pagesRead = 6)),
        )
        val viewModel = ProgressViewModel(sessions, FakeBookRepository(), goalStore, now = { now })
        viewModel.uiState.launchIn(backgroundScope)
        assertEquals(10, viewModel.uiState.value.stats!!.todayAmount)

        viewModel.setGoal(ReadingGoal(GoalUnit.PAGES, 12))

        val stats = viewModel.uiState.value.stats!!
        assertEquals(ReadingGoal(GoalUnit.PAGES, 12), stats.goal)
        assertEquals(6, stats.todayAmount)
        assertEquals(0.5f, stats.goalProgress, 0.001f)
    }

    @Test
    fun withNothingReadItIsEmpty() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ProgressViewModel(FakeReadingSessionRepository(), FakeBookRepository(), goalStore, now = { now })
        viewModel.uiState.launchIn(backgroundScope)

        assertTrue(viewModel.uiState.value.isEmpty)
    }
}
