package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.cloud.SyncError

/** What the user sees about the sync (T18.6). */
sealed interface SyncStatus {

    /** The user hasn't turned it on. */
    data object Disabled : SyncStatus

    data object Syncing : SyncStatus

    /** Everything is up to date as of [at] (epoch milliseconds), when it is known. */
    data class Synced(val at: Long?) : SyncStatus

    /** There are changes on this device the cloud doesn't have yet (no network, Wi-Fi wanted, or waiting its turn). */
    data object Pending : SyncStatus

    /** The last try failed; the user can try again. */
    data class Failed(val error: SyncError) : SyncStatus
}
