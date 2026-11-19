package io.github.fabiann1809.reader.data.sync

import androidx.work.ListenableWorker
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.testing.FakeSyncSettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorTest {

    private class FakeScheduler : SyncScheduler {
        data class Call(val wifiOnly: Boolean, val delayMillis: Long, val replace: Boolean)

        val calls = mutableListOf<Call>()
        var cancelled = 0

        override fun schedule(wifiOnly: Boolean, delayMillis: Long, replace: Boolean) {
            calls += Call(wifiOnly, delayMillis, replace)
        }

        override fun cancel() {
            cancelled++
        }
    }

    private class FakeChanges : LocalChangeSource {
        val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        var lastChange = 0L
        override val changes = flow
        override suspend fun lastChangeAt() = lastChange
    }

    private class FakeRepository : SyncRepository {
        var next: Result<SyncReport> = Result.success(SyncReport(0, 0, 0, false))
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun sync(): Result<SyncReport> {
            calls++
            gate?.await()
            return next
        }
    }

    private var now = 1_000L
    private val settings = FakeSyncSettingsStore()
    private val scheduler = FakeScheduler()
    private val changes = FakeChanges()
    private val repository = FakeRepository()

    private fun TestScope.coordinator() =
        SyncCoordinator(settings, repository, scheduler, changes, backgroundScope + UnconfinedTestDispatcher(testScheduler)) { now }

    private suspend fun SyncCoordinator.currentStatus() = status.first()

    @Test
    fun itIsOffUntilTheUserTurnsItOn() = runTest {
        val sync = coordinator()

        sync.onAppOpened()
        runCurrent()

        assertEquals(SyncStatus.Disabled, sync.currentStatus())
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun turningItOnSyncsAtOnceAndTurningItOffCancelsTheWork() = runTest {
        val sync = coordinator()

        sync.setEnabled(true)
        assertEquals(listOf(FakeScheduler.Call(wifiOnly = true, delayMillis = 0, replace = true)), scheduler.calls)

        sync.setEnabled(false)
        assertEquals(1, scheduler.cancelled)
        assertEquals(SyncStatus.Disabled, sync.currentStatus())
    }

    @Test
    fun openingTheAppSyncsWithoutReplacingOneThatIsWaiting() = runTest {
        settings.settings.value = SyncSettings(enabled = true, wifiOnly = false)
        val sync = coordinator()

        sync.onAppOpened()
        runCurrent()

        assertEquals(listOf(FakeScheduler.Call(wifiOnly = false, delayMillis = 0, replace = false)), scheduler.calls)
    }

    @Test
    fun aChangeSyncsAfterAShortWaitSoABurstMakesOneSync() = runTest {
        settings.settings.value = SyncSettings(enabled = true)
        val sync = coordinator()
        sync.onAppOpened()
        runCurrent()
        scheduler.calls.clear()

        repeat(5) {
            changes.flow.emit(Unit)
            advanceTimeBy(1_000)
        }
        advanceTimeBy(6_000)

        assertEquals(listOf(FakeScheduler.Call(wifiOnly = true, delayMillis = 5_000, replace = false)), scheduler.calls)
    }

    @Test
    fun changesWhileItIsOffScheduleNothing() = runTest {
        val sync = coordinator()
        sync.onAppOpened()
        runCurrent()

        changes.flow.emit(Unit)
        advanceTimeBy(10_000)

        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun aSuccessfulSyncRemembersWhenItBeganAndShowsSynced() = runTest {
        settings.settings.value = SyncSettings(enabled = true)
        changes.lastChange = 500
        val sync = coordinator()

        val result = sync.runSync()

        assertTrue(result.isSuccess)
        assertEquals(1_000L, settings.settings.value.lastSyncAt)
        assertEquals(SyncStatus.Synced(1_000L), sync.currentStatus())
    }

    @Test
    fun aFailureIsShownUntilTheUserTriesAgain() = runTest {
        settings.settings.value = SyncSettings(enabled = true)
        repository.next = Result.failure(SyncError.NotAuthorized())
        val sync = coordinator()

        val result = sync.runSync()

        assertTrue(result.exceptionOrNull() is SyncError.NotAuthorized)
        assertTrue((sync.currentStatus() as SyncStatus.Failed).error is SyncError.NotAuthorized)
        assertEquals(null, settings.settings.value.lastSyncAt)

        scheduler.calls.clear()
        sync.syncNow()

        assertEquals(1, scheduler.calls.size)
        assertEquals(SyncStatus.Synced(null), sync.currentStatus())
    }

    @Test
    fun changesNotYetSyncedShowAsPending() = runTest {
        settings.settings.value = SyncSettings(enabled = true, lastSyncAt = 100)
        changes.lastChange = 900
        val sync = coordinator()

        sync.onAppOpened()
        runCurrent()

        assertEquals(SyncStatus.Pending, sync.currentStatus())
    }

    @Test
    fun aChangeMadeWhileSyncingIsSyncedRightAfter() = runTest {
        settings.settings.value = SyncSettings(enabled = true)
        val sync = coordinator()
        repository.gate = CompletableDeferred()

        val job = launch { sync.runSync() }
        runCurrent()
        assertEquals(SyncStatus.Syncing, sync.currentStatus())

        // The user edits something at 1_500, after the sync began at 1_000.
        changes.lastChange = 1_500
        repository.gate!!.complete(Unit)
        job.join()

        assertEquals(SyncStatus.Pending, sync.currentStatus())
        assertEquals(listOf(FakeScheduler.Call(wifiOnly = true, delayMillis = 5_000, replace = false)), scheduler.calls)
    }

    @Test
    fun syncNowDoesNothingWhileASyncRuns() = runTest {
        settings.settings.value = SyncSettings(enabled = true)
        val sync = coordinator()
        repository.gate = CompletableDeferred()
        val job = launch { sync.runSync() }
        runCurrent()

        sync.syncNow()

        assertTrue(scheduler.calls.isEmpty())
        repository.gate!!.complete(Unit)
        job.join()
    }

    @Test
    fun theBackgroundJobDoesNotSyncWhenItWasTurnedOff() = runTest {
        val sync = coordinator()

        val result = sync.runSync()

        assertTrue(result.isSuccess)
        assertEquals(0, repository.calls)
    }

    @Test
    fun changingTheWifiRuleSyncsAgainWithTheNewOne() = runTest {
        settings.settings.value = SyncSettings(enabled = true, wifiOnly = true)
        val sync = coordinator()

        sync.setWifiOnly(false)

        assertEquals(listOf(FakeScheduler.Call(wifiOnly = false, delayMillis = 0, replace = true)), scheduler.calls)
        assertEquals(false, settings.settings.value.wifiOnly)
    }

    @Test
    fun onlyTemporaryProblemsAreRetried() {
        assertTrue(resultFor(null, 0) is ListenableWorker.Result.Success)
        assertTrue(resultFor(SyncError.Unknown("x"), 0) is ListenableWorker.Result.Retry)
        assertTrue(resultFor(SyncError.NoInternet(java.io.IOException()), 2) is ListenableWorker.Result.Retry)
        assertTrue(resultFor(SyncError.Conflict(), 1) is ListenableWorker.Result.Retry)
        assertTrue(resultFor(SyncError.NotAuthorized(), 0) is ListenableWorker.Result.Failure)
        assertTrue(resultFor(SyncError.QuotaExceeded(), 0) is ListenableWorker.Result.Failure)
        assertTrue(resultFor(SyncError.NewerFormat(), 0) is ListenableWorker.Result.Failure)
        assertTrue(resultFor(SyncError.Unknown("x"), 5) is ListenableWorker.Result.Failure)
    }
}
