package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the sync screen asks of the sync (T18.7). */
interface SyncControl {
    val status: Flow<SyncStatus>

    /** Turns the sync on (and syncs at once) or off. */
    suspend fun setEnabled(enabled: Boolean)

    suspend fun setWifiOnly(wifiOnly: Boolean)

    /** "Sincronizar ahora" and "Reintentar". */
    suspend fun syncNow()

    /**
     * Before signing out (T18.8): with the sync on, saves everything to the cloud and, only if that
     * went well and nothing is left to upload, empties this device and turns the sync off. If it
     * fails nothing is erased, and the sign-out must not go on. With the sync off it does nothing.
     * [userId] is the account signing out: it is remembered, so its library comes back when it signs in again (T18.9b).
     */
    suspend fun saveBeforeSignOut(userId: String?): Result<Unit>
}

/**
 * Decides when the sync runs and tells how it is going (T18.6). It syncs when the app opens, a few
 * seconds after the data changes, and when the user asks; WorkManager runs it, waiting for the network.
 */
class SyncCoordinator(
    private val settings: SyncSettingsStore,
    private val repository: SyncRepository,
    private val scheduler: SyncScheduler,
    private val changeSource: LocalChangeSource,
    private val eraser: LocalDataEraser,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : SyncControl {
    private val running = MutableStateFlow(false)
    private val lastError = MutableStateFlow<SyncError?>(null)
    private val hasPending = MutableStateFlow(false)
    private var started = false

    override val status: Flow<SyncStatus> = combine(settings.settings, running, lastError, hasPending) { saved, isRunning, error, pending ->
        when {
            !saved.enabled -> SyncStatus.Disabled
            isRunning -> SyncStatus.Syncing
            error != null -> SyncStatus.Failed(error)
            pending -> SyncStatus.Pending
            else -> SyncStatus.Synced(saved.lastSyncAt)
        }
    }

    /** The app came to the front: starts watching the data (once) and syncs if it is on. */
    fun onAppOpened() {
        start()
        scope.launch {
            val saved = settings.settings.first()
            if (saved.enabled) {
                refreshPending()
                scheduler.schedule(saved.wifiOnly, delayMillis = 0, replace = false)
            }
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        settings.update { it.copy(enabled = enabled) }
        if (enabled) {
            lastError.value = null
            refreshPending()
            syncNow()
        } else {
            scheduler.cancel()
        }
    }

    override suspend fun setWifiOnly(wifiOnly: Boolean) {
        settings.update { it.copy(wifiOnly = wifiOnly) }
        // The waiting sync was asked with the old rule.
        if (settings.settings.first().enabled) syncNow()
    }

    // A sync that is running already covers it.
    override suspend fun syncNow() {
        val saved = settings.settings.first()
        if (!saved.enabled || running.value) return
        lastError.value = null
        scheduler.schedule(saved.wifiOnly, delayMillis = 0, replace = true)
    }

    override suspend fun saveBeforeSignOut(userId: String?): Result<Unit> {
        // Nothing is in the cloud, so nothing may be erased.
        if (!settings.settings.first().enabled) return Result.success(Unit)
        // A sync that applies a deletion leaves a fresh tombstone, so the first pass can still show changes: one more settles it.
        repeat(SAVE_PASSES) {
            runSync().onFailure { return Result.failure(it) }
            if (!hasPending.value) return erase(userId)
        }
        return Result.failure(SyncError.Unknown("The data kept changing while it was saved"))
    }

    private suspend fun erase(userId: String?): Result<Unit> = runCatching {
        eraser.eraseAll()
        scheduler.cancel()
        settings.update { it.copy(enabled = false, lastSyncAt = null, restoreFor = it.restoreFor + listOfNotNull(userId)) }
        lastError.value = null
        hasPending.value = false
    }

    /** What the background job runs. */
    suspend fun runSync(): Result<SyncReport> {
        if (!settings.settings.first().enabled) return Result.success(SyncReport(0, 0, 0, false))
        // The time it began, so a change made while it runs still counts as pending afterwards.
        val startedAt = clock()
        running.value = true
        val result = try {
            repository.sync()
        } finally {
            running.value = false
        }
        result.onSuccess {
            settings.update { it.copy(lastSyncAt = startedAt) }
            lastError.value = null
        }.onFailure { lastError.value = it as? SyncError ?: SyncError.Unknown("The sync failed", it) }
        refreshPending()
        if (result.isSuccess && hasPending.value) scheduleForChanges()
        return result
    }

    private fun start() {
        if (started) return
        started = true
        scope.launch { changeSource.changes.debounceChanges().collect { onLocalChange() } }
    }

    @OptIn(FlowPreview::class)
    private fun Flow<Unit>.debounceChanges() = debounce(CHANGE_DELAY_MILLIS)

    private suspend fun onLocalChange() {
        if (!settings.settings.first().enabled) return
        refreshPending()
        scheduleForChanges()
    }

    private suspend fun scheduleForChanges() {
        val saved = settings.settings.first()
        if (saved.enabled) scheduler.schedule(saved.wifiOnly, CHANGE_DELAY_MILLIS, replace = false)
    }

    private suspend fun refreshPending() {
        val lastSync = settings.settings.first().lastSyncAt ?: 0L
        hasPending.value = changeSource.lastChangeAt() > lastSync
    }

    private companion object {
        // Waits for a burst of changes (reading saves progress often) to end before syncing.
        const val CHANGE_DELAY_MILLIS = 5_000L
        const val SAVE_PASSES = 3
    }
}
