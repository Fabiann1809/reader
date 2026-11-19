package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.sync.SyncSettings
import io.github.fabiann1809.reader.data.sync.SyncSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSyncSettingsStore(initial: SyncSettings = SyncSettings()) : SyncSettingsStore {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(change: (SyncSettings) -> SyncSettings) = settings.update(change)
}
