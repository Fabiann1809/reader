package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The reader's "Aa" settings (T11.7), kept between launches. */
interface ReadingPreferences {
    val settings: Flow<ReadingSettings>

    suspend fun setSettings(settings: ReadingSettings)
}

// Only one DataStore instance may exist per file, so AppContainer keeps this as a singleton.
// Tests pass their own [fileName] so they never touch the app's settings.
class DataStoreReadingPreferences(context: Context, fileName: String = FILE_NAME) : ReadingPreferences {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val settings: Flow<ReadingSettings> = dataStore.data.map { prefs ->
        val defaults = ReadingSettings()
        ReadingSettings(
            theme = enumOrNull<ReadingTheme>(prefs[THEME]) ?: defaults.theme,
            font = enumOrNull<ReadingFont>(prefs[FONT]) ?: defaults.font,
            fontSize = prefs[FONT_SIZE]?.coerceIn(ReadingSettings.FontSizeRange) ?: defaults.fontSize,
            lineHeight = prefs[LINE_HEIGHT]?.coerceIn(ReadingSettings.LineHeightRange) ?: defaults.lineHeight,
            margins = prefs[MARGINS]?.coerceIn(ReadingSettings.MarginsRange) ?: defaults.margins,
            alignment = enumOrNull<ReadingAlignment>(prefs[ALIGNMENT]) ?: defaults.alignment,
        )
    }

    override suspend fun setSettings(settings: ReadingSettings) {
        dataStore.edit { prefs ->
            prefs[THEME] = settings.theme.name
            prefs[FONT] = settings.font.name
            prefs[FONT_SIZE] = settings.fontSize
            prefs[LINE_HEIGHT] = settings.lineHeight
            prefs[MARGINS] = settings.margins
            prefs[ALIGNMENT] = settings.alignment.name
        }
    }

    private companion object {
        const val FILE_NAME = "reading_preferences"
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font")
        val FONT_SIZE = doublePreferencesKey("font_size")
        val LINE_HEIGHT = doublePreferencesKey("line_height")
        val MARGINS = doublePreferencesKey("margins")
        val ALIGNMENT = stringPreferencesKey("alignment")
    }
}
