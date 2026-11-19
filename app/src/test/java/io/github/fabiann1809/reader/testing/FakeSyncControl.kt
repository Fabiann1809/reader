package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.sync.SyncControl
import io.github.fabiann1809.reader.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow

/** A [SyncControl] that records what the screen asked and shows the [status] the test sets. */
class FakeSyncControl(initial: SyncStatus = SyncStatus.Disabled) : SyncControl {

    override val status = MutableStateFlow(initial)
    val calls = mutableListOf<String>()

    /** What saving before signing out answers, and for which account it was asked. */
    var saveResult: Result<Unit> = Result.success(Unit)
    var savedFor: String? = null

    /** What the status becomes once the sync is turned on, as if its first run had ended that way. */
    var statusAfterEnable: SyncStatus = SyncStatus.Synced(null)

    override suspend fun setEnabled(enabled: Boolean) {
        calls += "enabled:$enabled"
        status.value = if (enabled) statusAfterEnable else SyncStatus.Disabled
    }

    override suspend fun setWifiOnly(wifiOnly: Boolean) {
        calls += "wifiOnly:$wifiOnly"
    }

    override suspend fun syncNow() {
        calls += "syncNow"
    }

    override suspend fun saveBeforeSignOut(userId: String?): Result<Unit> {
        calls += "saveBeforeSignOut"
        savedFor = userId
        return saveResult
    }
}
