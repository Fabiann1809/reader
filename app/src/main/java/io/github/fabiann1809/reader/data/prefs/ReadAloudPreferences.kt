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

/** How the book is read aloud (T11.15): the speed (1 = normal) and the voice the user picked, if any. */
data class ReadAloudSettings(val speed: Double = NORMAL_SPEED, val voiceId: String? = null) {
    companion object {
        const val NORMAL_SPEED = 1.0
    }
}

/** The read aloud settings, kept between launches. */
interface ReadAloudPreferences {
    val settings: Flow<ReadAloudSettings>

    suspend fun setSpeed(speed: Double)

    suspend fun setVoice(voiceId: String)
}

// Only one DataStore instance may exist per file, so AppContainer keeps this as a singleton.
// Tests pass their own [fileName] so they never touch the app's settings.
class DataStoreReadAloudPreferences(context: Context, fileName: String = FILE_NAME) : ReadAloudPreferences {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val settings: Flow<ReadAloudSettings> = dataStore.data.map { prefs ->
        ReadAloudSettings(speed = prefs[SPEED] ?: ReadAloudSettings.NORMAL_SPEED, voiceId = prefs[VOICE])
    }

    override suspend fun setSpeed(speed: Double) {
        dataStore.edit { it[SPEED] = speed }
    }

    override suspend fun setVoice(voiceId: String) {
        dataStore.edit { it[VOICE] = voiceId }
    }

    private companion object {
        const val FILE_NAME = "read_aloud"
        val SPEED = doublePreferencesKey("speed")
        val VOICE = stringPreferencesKey("voice")
    }
}
