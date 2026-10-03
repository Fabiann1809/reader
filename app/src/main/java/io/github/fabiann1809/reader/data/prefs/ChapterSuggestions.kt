package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first

/** Remembers which chapters already got the end-of-chapter suggestion (T15.5): it shows once per chapter. */
interface ChapterSuggestions {
    suspend fun wasSuggested(bookId: Long, href: String): Boolean

    suspend fun markSuggested(bookId: Long, href: String)
}

// Tests pass their own [fileName] so they never touch the app's data.
class DataStoreChapterSuggestions(context: Context, fileName: String = FILE_NAME) : ChapterSuggestions {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override suspend fun wasSuggested(bookId: Long, href: String): Boolean =
        key(bookId, href) in dataStore.data.first()[SUGGESTED].orEmpty()

    override suspend fun markSuggested(bookId: Long, href: String) {
        dataStore.edit { prefs -> prefs[SUGGESTED] = prefs[SUGGESTED].orEmpty() + key(bookId, href) }
    }

    private fun key(bookId: Long, href: String) = "$bookId:$href"

    private companion object {
        const val FILE_NAME = "chapter_suggestions"
        val SUGGESTED = stringSetPreferencesKey("suggested")
    }
}
