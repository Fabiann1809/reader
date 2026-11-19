package io.github.fabiann1809.reader.data.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the user chose about the sync, and when it last worked (T18.6). */
data class SyncSettings(
    // Off until the user connects Drive and turns it on.
    val enabled: Boolean = false,
    // Syncing moves books, so by default it waits for Wi-Fi.
    val wifiOnly: Boolean = true,
    // Epoch milliseconds of when the last successful sync began; null if there was none.
    val lastSyncAt: Long? = null,
    // The Reader account (its uid) whose library was saved to Drive when it signed out and emptied
    // this phone (T18.9b): when that account signs in again, the library comes back by itself.
    val restoreFor: String? = null,
)

interface SyncSettingsStore {
    val settings: Flow<SyncSettings>

    suspend fun update(change: (SyncSettings) -> SyncSettings)
}

// Tests pass their own [fileName] so they never touch the app's data.
class DataStoreSyncSettingsStore(context: Context, fileName: String = FILE_NAME) : SyncSettingsStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val settings: Flow<SyncSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(change: (SyncSettings) -> SyncSettings) {
        dataStore.edit { prefs ->
            val updated = change(prefs.toSettings())
            prefs[ENABLED] = updated.enabled
            prefs[WIFI_ONLY] = updated.wifiOnly
            prefs.putOrRemove(LAST_SYNC_AT, updated.lastSyncAt)
            prefs.putOrRemove(RESTORE_FOR, updated.restoreFor)
        }
    }

    private fun Preferences.toSettings() = SyncSettings(
        enabled = this[ENABLED] ?: false,
        wifiOnly = this[WIFI_ONLY] ?: true,
        lastSyncAt = this[LAST_SYNC_AT],
        restoreFor = this[RESTORE_FOR],
    )

    private fun <T> androidx.datastore.preferences.core.MutablePreferences.putOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        const val FILE_NAME = "sync_settings"
        val ENABLED = booleanPreferencesKey("enabled")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val RESTORE_FOR = stringPreferencesKey("restore_for")
    }
}
