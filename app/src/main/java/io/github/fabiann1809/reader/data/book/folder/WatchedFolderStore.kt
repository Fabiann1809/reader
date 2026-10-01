package io.github.fabiann1809.reader.data.book.folder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A folder the user chose to watch: its tree URI and the name to show. */
data class WatchedFolder(val uri: String, val name: String)

/** The watched folder and which of its files were already handled, so each file is imported once. */
interface WatchedFolderStore {
    val folder: Flow<WatchedFolder?>

    /** Also forgets the handled files, which belonged to the previous folder. */
    suspend fun setFolder(folder: WatchedFolder?)

    suspend fun handledFiles(): Set<String>

    suspend fun markHandled(fileUris: Collection<String>)
}

/** Its own small DataStore file, independent from the app settings. One instance per file (see AppContainer). */
class DataStoreWatchedFolderStore(context: Context, fileName: String = "watched_folder") : WatchedFolderStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val folder: Flow<WatchedFolder?> = dataStore.data.map { prefs ->
        val uri = prefs[FOLDER_URI] ?: return@map null
        WatchedFolder(uri, prefs[FOLDER_NAME].orEmpty())
    }

    override suspend fun setFolder(folder: WatchedFolder?) {
        dataStore.edit { prefs ->
            prefs.remove(HANDLED)
            if (folder == null) {
                prefs.remove(FOLDER_URI)
                prefs.remove(FOLDER_NAME)
            } else {
                prefs[FOLDER_URI] = folder.uri
                prefs[FOLDER_NAME] = folder.name
            }
        }
    }

    override suspend fun handledFiles(): Set<String> = dataStore.data.first()[HANDLED].orEmpty()

    override suspend fun markHandled(fileUris: Collection<String>) {
        if (fileUris.isEmpty()) return
        dataStore.edit { prefs -> prefs[HANDLED] = prefs[HANDLED].orEmpty() + fileUris }
    }

    private companion object {
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val FOLDER_NAME = stringPreferencesKey("folder_name")
        val HANDLED = stringSetPreferencesKey("handled_files")
    }
}
