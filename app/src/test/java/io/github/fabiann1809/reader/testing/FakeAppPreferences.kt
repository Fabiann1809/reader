package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory AppPreferences for ViewModel tests. */
class FakeAppPreferences(seenOnboarding: Boolean = false) : AppPreferences {

    private val seen = MutableStateFlow(seenOnboarding)

    override val hasSeenOnboarding: Flow<Boolean> = seen

    override suspend fun markOnboardingSeen() {
        seen.value = true
    }

    private val filter = MutableStateFlow<LibraryFilter>(LibraryFilter.Default)

    override val libraryFilter: Flow<LibraryFilter> = filter

    override suspend fun setLibraryFilter(filter: LibraryFilter) {
        this.filter.value = filter
    }

    private val arrangement = MutableStateFlow(LibraryArrangement())

    override val libraryArrangement: Flow<LibraryArrangement> = arrangement

    override suspend fun setLibraryArrangement(arrangement: LibraryArrangement) {
        this.arrangement.value = arrangement
    }
}
