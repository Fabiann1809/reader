package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.prefs.AppSettings
import io.github.fabiann1809.reader.data.prefs.AppSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeAppSettingsStore(initial: AppSettings = AppSettings()) : AppSettingsStore {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(change: (AppSettings) -> AppSettings) = settings.update(change)
}
