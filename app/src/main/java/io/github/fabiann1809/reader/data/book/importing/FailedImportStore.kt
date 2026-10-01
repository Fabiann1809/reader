package io.github.fabiann1809.reader.data.book.importing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Remembers the files of the last batch that could not be imported, so the error card survives a restart. */
interface FailedImportStore {
    suspend fun load(): List<FailedImport>

    /** An empty list clears it. */
    suspend fun save(failures: List<FailedImport>)
}

/**
 * Its own small file, so it stays independent from the app settings. Only one instance per file may
 * exist (DataStore rule), so the app keeps it in AppContainer; tests pass another [fileName].
 */
class DataStoreFailedImportStore(context: Context, fileName: String = "failed_imports") : FailedImportStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override suspend fun load(): List<FailedImport> {
        val json = dataStore.data.first()[FAILURES] ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (e: SerializationException) {
            // Saved by a version with a different format: nothing worth crashing over.
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    override suspend fun save(failures: List<FailedImport>) {
        dataStore.edit { prefs ->
            if (failures.isEmpty()) prefs.remove(FAILURES) else prefs[FAILURES] = Json.encodeToString(failures)
        }
    }

    private companion object {
        val FAILURES = stringPreferencesKey("failures")
    }
}
