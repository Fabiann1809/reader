package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.sync.SyncControl
import io.github.fabiann1809.reader.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow

/** A [SyncControl] that records what the screen asked and shows the [status] the test sets. */
class FakeSyncControl(initial: SyncStatus = SyncStatus.Disabled) : SyncControl {

    override val status = MutableStateFlow(initial)
    val calls = mutableListOf<String>()

    override suspend fun setEnabled(enabled: Boolean) {
        calls += "enabled:$enabled"
        status.value = if (enabled) SyncStatus.Synced(null) else SyncStatus.Disabled
    }

    override suspend fun setWifiOnly(wifiOnly: Boolean) {
        calls += "wifiOnly:$wifiOnly"
    }

    override suspend fun syncNow() {
        calls += "syncNow"
    }
}
