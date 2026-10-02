package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.prefs.ReadingPreferences
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory ReadingPreferences for ViewModel tests. */
class FakeReadingPreferences(initial: ReadingSettings = ReadingSettings()) : ReadingPreferences {

    override val settings = MutableStateFlow(initial)

    override suspend fun setSettings(settings: ReadingSettings) {
        this.settings.value = settings
    }
}
