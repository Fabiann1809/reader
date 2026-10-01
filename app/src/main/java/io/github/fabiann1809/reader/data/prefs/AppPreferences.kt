package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Small app-wide settings that are not part of the user's library (kept out of Room on purpose). */
interface AppPreferences {
    /** True once the user finished or skipped the first-run onboarding. */
    val hasSeenOnboarding: Flow<Boolean>

    suspend fun markOnboardingSeen()
}

// Only one DataStore instance may exist per file, so AppContainer keeps this as a singleton.
class DataStoreAppPreferences(context: Context) : AppPreferences {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(FILE_NAME) },
    )

    override val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_SEEN] ?: false }

    override suspend fun markOnboardingSeen() {
        dataStore.edit { it[ONBOARDING_SEEN] = true }
    }

    private companion object {
        const val FILE_NAME = "app_preferences"
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }
}
