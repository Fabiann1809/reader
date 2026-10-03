package io.github.fabiann1809.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.data.prefs.AppSettings
import io.github.fabiann1809.reader.data.prefs.AppSettingsStore
import io.github.fabiann1809.reader.data.prefs.AppTheme
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.data.prefs.ReadingPreferences
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
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
