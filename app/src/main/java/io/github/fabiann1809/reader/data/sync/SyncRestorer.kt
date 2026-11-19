package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.auth.DriveAuthorizer
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.cloud.CloudStorage
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import kotlinx.coroutines.flow.first

/**
 * Brings a library back when its account signs in again (T18.9b). Signing out with the sync on saved
 * everything to Drive and emptied the phone (T18.8); here the same account gets it back without going to
 * Ajustes. Another account, or a phone that already holds data, restores nothing: nobody gets a library
 * that isn't theirs, and nothing local is mixed into someone else's Drive.
 */
class SyncRestorer(
    private val settings: SyncSettingsStore,
    private val authorizer: DriveAuthorizer,
    private val cloud: CloudStorage,
    private val sync: SyncControl,
    private val changeSource: LocalChangeSource,
) {
    /** Whether the library of [userId] is waiting in Drive and this phone is empty. */
    suspend fun isPending(userId: String): Boolean {
        if (userId !in settings.settings.first().restoreFor) return false
        if (changeSource.lastChangeAt() != 0L) {
            // The phone has data of its own now: restoring would mix it with the cloud's.
            clear(userId)
            return false
        }
        return true
    }

    /**
     * Reconnects Drive (the permission is already given, so nothing is asked), turns the sync on and waits
     * until its first run ends. Fails with the reason when it can't; the caller can retry or go on without it.
     */
    suspend fun restore(userId: String): Result<Unit> {
        when (authorizer.authorize()) {
            is DriveGrant.Granted -> Unit
            // The user would have to accept again, which belongs to Ajustes, not to a loading screen.
            is DriveGrant.NeedsConsent, DriveGrant.Declined -> return Result.failure(SyncError.NotAuthorized())
            is DriveGrant.Failed -> return Result.failure(SyncError.Unknown("Google could not give the Drive permission"))
        }
        cloud.listFiles().onFailure { return Result.failure(it) }
        sync.setEnabled(true)
        val end = sync.status.first { it is SyncStatus.Failed || (it is SyncStatus.Synced && it.at != null) }
        if (end is SyncStatus.Failed) return Result.failure(end.error)
        clear(userId)
        return Result.success(Unit)
    }

    /** Stops expecting [userId]'s restore: it is done, or the user went on without it. */
    suspend fun clear(userId: String) {
        settings.update { it.copy(restoreFor = it.restoreFor - userId) }
    }
}
