package io.github.fabiann1809.reader.ui.settings

import io.github.fabiann1809.reader.data.prefs.AppTheme
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.data.prefs.ReadingTheme
import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.FakeAppSettingsStore
import io.github.fabiann1809.reader.testing.FakeReadingPreferences
import io.github.fabiann1809.reader.testing.FakeReviewReminderScheduler
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GeneralSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appSettings = FakeAppSettingsStore()
    private val appPreferences = FakeAppPreferences()
    private val readingPreferences = FakeReadingPreferences()
    private val reminderScheduler = FakeReviewReminderScheduler()

    private fun viewModel() = GeneralSettingsViewModel(appSettings, appPreferences, readingPreferences, reminderScheduler)

    @Test
    fun eachSettingIsSavedAndShown() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setTheme(AppTheme.DARK)
        viewModel.setAutoTranscribe(false)
        viewModel.setChapterEndSuggestion(false)
        viewModel.setLibraryView(LibraryView.COLLECTIONS)
        viewModel.setBooksPerRow(9)

        val state = viewModel.uiState.value
        assertEquals(AppTheme.DARK, state.settings.theme)
        assertFalse(state.settings.autoTranscribe)
        assertFalse(state.settings.chapterEndSuggestion)
        assertEquals(LibraryView.COLLECTIONS, state.layout.view)
        // Out of range: kept to the largest count a row allows.
        assertEquals(4, state.layout.booksPerRow)
    }

    @Test
    fun theReadingDefaultsAreTheReadersAaSettings() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.showReadingSettings()
        assertTrue(viewModel.uiState.value.readingSheetVisible)
        viewModel.updateReadingSettings { it.copy(theme = ReadingTheme.SEPIA) }
        viewModel.hideReadingSettings()

        assertEquals(ReadingTheme.SEPIA, readingPreferences.settings.value.theme)
        assertFalse(viewModel.uiState.value.readingSheetVisible)
    }

    @Test
    fun turningTheReminderOnSetsItAtTheChosenHourAndOffCancelsIt() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setReviewReminder(true)
        // 8 p. m. until another hour is picked.
        assertEquals(listOf(20 * 60), reminderScheduler.scheduled)
        assertTrue(viewModel.uiState.value.settings.reviewReminder)

        viewModel.setReviewReminder(false)
        assertTrue(reminderScheduler.cancelled)
        assertFalse(viewModel.uiState.value.settings.reviewReminder)
    }

    @Test
    fun changingTheHourMovesTheReminderOnlyWhileItIsOn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setReminderTime(7 * 60 + 15)
        assertTrue(reminderScheduler.scheduled.isEmpty())
        assertEquals(7 * 60 + 15, viewModel.uiState.value.settings.reminderMinutes)

        viewModel.setReviewReminder(true)
        viewModel.setReminderTime(9 * 60)

        assertEquals(listOf(7 * 60 + 15, 9 * 60), reminderScheduler.scheduled)
    }
}
