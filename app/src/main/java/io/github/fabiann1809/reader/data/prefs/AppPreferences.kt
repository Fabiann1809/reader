package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookSort
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Small app-wide settings that are not part of the user's library (kept out of Room on purpose). */
interface AppPreferences {
    /** True once the user finished or skipped the first-run onboarding. */
    val hasSeenOnboarding: Flow<Boolean>

    suspend fun markOnboardingSeen()

    /** Collection shown in the library, remembered between launches. */
    val libraryFilter: Flow<LibraryFilter>

    suspend fun setLibraryFilter(filter: LibraryFilter)

    /** Shelf order and filters, remembered between launches. */
    val libraryArrangement: Flow<LibraryArrangement>

    suspend fun setLibraryArrangement(arrangement: LibraryArrangement)

    /** Shelves, grid or list, and books per row, remembered between launches. */
    val libraryLayout: Flow<LibraryLayout>

    suspend fun setLibraryLayout(layout: LibraryLayout)
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

    override val libraryFilter: Flow<LibraryFilter> = dataStore.data.map { LibraryFilter.decode(it[LIBRARY_FILTER]) }

    override suspend fun setLibraryFilter(filter: LibraryFilter) {
        dataStore.edit { it[LIBRARY_FILTER] = filter.encode() }
    }

    override val libraryArrangement: Flow<LibraryArrangement> = dataStore.data.map { prefs ->
        LibraryArrangement(
            sort = enumOrNull<BookSort>(prefs[LIBRARY_SORT]) ?: LibraryArrangement().sort,
            statuses = prefs[LIBRARY_STATUSES].toEnumSet(),
            formats = prefs[LIBRARY_FORMATS].toEnumSet(),
            kinds = prefs[LIBRARY_KINDS].toEnumSet(),
        )
    }

    override suspend fun setLibraryArrangement(arrangement: LibraryArrangement) {
        dataStore.edit { prefs ->
            prefs[LIBRARY_SORT] = arrangement.sort.name
            prefs[LIBRARY_STATUSES] = arrangement.statuses.names()
            prefs[LIBRARY_FORMATS] = arrangement.formats.names()
            prefs[LIBRARY_KINDS] = arrangement.kinds.names()
        }
    }

    override val libraryLayout: Flow<LibraryLayout> = dataStore.data.map { prefs ->
        LibraryLayout(
            view = enumOrNull<LibraryView>(prefs[LIBRARY_VIEW]) ?: LibraryLayout().view,
            booksPerRow = prefs[LIBRARY_BOOKS_PER_ROW]?.takeIf { it in LibraryLayout.BooksPerRowRange }
                ?: LibraryLayout.DEFAULT_BOOKS_PER_ROW,
        )
    }

    override suspend fun setLibraryLayout(layout: LibraryLayout) {
        dataStore.edit { prefs ->
            prefs[LIBRARY_VIEW] = layout.view.name
            prefs[LIBRARY_BOOKS_PER_ROW] = layout.booksPerRow.coerceIn(LibraryLayout.BooksPerRowRange)
        }
    }

    private companion object {
        const val FILE_NAME = "app_preferences"
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_STATUSES = stringSetPreferencesKey("library_statuses")
        val LIBRARY_FORMATS = stringSetPreferencesKey("library_formats")
        val LIBRARY_KINDS = stringSetPreferencesKey("library_kinds")
        val LIBRARY_VIEW = stringPreferencesKey("library_view")
        val LIBRARY_BOOKS_PER_ROW = intPreferencesKey("library_books_per_row")
    }
}

// Enums are stored by name; names unknown to this version (e.g. from a newer one) are dropped.
internal inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? = enumValues<E>().firstOrNull { it.name == name }

private inline fun <reified E : Enum<E>> Set<String>?.toEnumSet(): Set<E> =
    orEmpty().mapNotNull { enumOrNull<E>(it) }.toSet()

private fun Set<Enum<*>>.names(): Set<String> = mapTo(mutableSetOf()) { it.name }
