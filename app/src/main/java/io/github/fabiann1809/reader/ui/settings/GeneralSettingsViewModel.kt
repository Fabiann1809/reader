package io.github.fabiann1809.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.data.prefs.AppSettings
import io.github.fabiann1809.reader.data.prefs.AppSettingsStore
import io.github.fabiann1809.reader.data.prefs.AppTheme
import io.github.fabiann1809.reader.data.prefs.current
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.data.prefs.ReadingPreferences
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.reminder.ReviewReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GeneralSettingsUiState(
    val settings: AppSettings = AppSettings(),
    val layout: LibraryLayout = LibraryLayout(),
    val reading: ReadingSettings = ReadingSettings(),
    val readingSheetVisible: Boolean = false,
)

/**
 * Ajustes (T17.1) beyond the API key: the app's theme, the library's default view, the reading
 * defaults (the reader's "Aa" settings) and the AI features that run without being asked.
 */
class GeneralSettingsViewModel(
    private val appSettings: AppSettingsStore,
    private val appPreferences: AppPreferences,
    private val readingPreferences: ReadingPreferences,
    private val reminderScheduler: ReviewReminderScheduler,
) : ViewModel() {

    private val readingSheetVisible = MutableStateFlow(false)

    val uiState: StateFlow<GeneralSettingsUiState> =
        combine(appSettings.settings, appPreferences.libraryLayout, readingPreferences.settings, readingSheetVisible) {
                settings, layout, reading, sheet ->
            GeneralSettingsUiState(settings, layout, reading, sheet)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), GeneralSettingsUiState())

    fun setTheme(theme: AppTheme) = updateSettings { it.copy(theme = theme) }

    fun setAutoTranscribe(on: Boolean) = updateSettings { it.copy(autoTranscribe = on) }

    fun setChapterEndSuggestion(on: Boolean) = updateSettings { it.copy(chapterEndSuggestion = on) }

    /** Turns the daily review reminder on or off; on, the first one is set for the chosen hour. */
    fun setReviewReminder(on: Boolean) {
        viewModelScope.launch {
            appSettings.update { it.copy(reviewReminder = on) }
            if (on) reminderScheduler.schedule(appSettings.current().reminderMinutes) else reminderScheduler.cancel()
        }
    }

    fun setReminderTime(minutesOfDay: Int) {
        viewModelScope.launch {
            appSettings.update { it.copy(reminderMinutes = minutesOfDay) }
            if (appSettings.current().reviewReminder) reminderScheduler.schedule(minutesOfDay)
        }
    }

    fun setLibraryView(view: LibraryView) = updateLayout { it.copy(view = view) }

    fun setBooksPerRow(count: Int) = updateLayout { it.copy(booksPerRow = count.coerceIn(LibraryLayout.BooksPerRowRange)) }

    fun showReadingSettings() {
        readingSheetVisible.value = true
    }

    fun hideReadingSettings() {
        readingSheetVisible.value = false
    }

    /** A change in the "Aa" sheet: the same defaults every book opens with. */
    fun updateReadingSettings(change: (ReadingSettings) -> ReadingSettings) {
        viewModelScope.launch { readingPreferences.setSettings(change(readingPreferences.settings.first())) }
    }

    private fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { appSettings.update(change) }
    }

    private fun updateLayout(change: (LibraryLayout) -> LibraryLayout) {
        viewModelScope.launch { appPreferences.setLibraryLayout(change(appPreferences.libraryLayout.first())) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
