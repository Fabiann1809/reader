package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The app's light or dark look (T17.1): as the phone's, or always one of them. */
enum class AppTheme { SYSTEM, LIGHT, DARK }

/**
 * Settings of Ajustes (T17.1) that don't belong to the library or the reader: the app's theme and
 * the AI features that run without being asked, which can be turned off.
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    // A voice note is written down by the AI as soon as it's recorded (T13.2); off, only on request.
    val autoTranscribe: Boolean = true,
    // "¿Quieres repasar o probarte?" when a chapter ends (T15.5).
    val chapterEndSuggestion: Boolean = true,
)

interface AppSettingsStore {
    val settings: Flow<AppSettings>

    suspend fun update(change: (AppSettings) -> AppSettings)
}

/** The settings as they are now, for code that only needs them once. */
suspend fun AppSettingsStore.current(): AppSettings = settings.first()

// Tests pass their own [fileName] so they never touch the app's data.
class DataStoreAppSettingsStore(context: Context, fileName: String = FILE_NAME) : AppSettingsStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(change: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = change(prefs.toSettings())
            prefs[THEME] = updated.theme.name
            prefs[AUTO_TRANSCRIBE] = updated.autoTranscribe
            prefs[CHAPTER_END_SUGGESTION] = updated.chapterEndSuggestion
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = AppTheme.entries.find { it.name == this[THEME] } ?: defaults.theme,
            autoTranscribe = this[AUTO_TRANSCRIBE] ?: defaults.autoTranscribe,
            chapterEndSuggestion = this[CHAPTER_END_SUGGESTION] ?: defaults.chapterEndSuggestion,
        )
    }

    private companion object {
        const val FILE_NAME = "app_settings"
        val THEME = stringPreferencesKey("theme")
        val AUTO_TRANSCRIBE = booleanPreferencesKey("auto_transcribe")
        val CHAPTER_END_SUGGESTION = booleanPreferencesKey("chapter_end_suggestion")
    }
}
