package io.github.fabiann1809.reader.ui.account

import io.github.fabiann1809.reader.data.sync.SyncManifest
import io.github.fabiann1809.reader.data.sync.SyncSettings
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.auth.Consent
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.auth.FailureCause
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import io.github.fabiann1809.reader.testing.FakeDriveAuthorizer
import io.github.fabiann1809.reader.testing.FakeSyncControl
import io.github.fabiann1809.reader.testing.FakeSyncSettingsStore
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SyncViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authorizer = FakeDriveAuthorizer()
    private val cloud = FakeCloudStorage()
    private val sync = FakeSyncControl()
    private val settings = FakeSyncSettingsStore()
    private val consent = Consent { error("Not opened in unit tests") }

    private fun viewModel() = SyncViewModel(authorizer, cloud, sync, settings)

    @Test
    fun startsOff() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(SyncStatus.Disabled, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.wifiOnly)
    }

    @Test
    fun turningItOnWithThePermissionAlreadyGivenTurnsTheSyncOn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertEquals(listOf("enabled:true"), sync.calls)
        assertFalse(viewModel.uiState.value.connecting)
        assertTrue(viewModel.uiState.value.status is SyncStatus.Synced)
    }

    @Test
    fun theFirstTimeItAsksForConsentAndTurnsOnAfterTheUserAccepts() = runTest(mainDispatcherRule.testDispatcher) {
        authorizer.onAuthorize = DriveGrant.NeedsConsent(consent)
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertEquals(consent, viewModel.uiState.value.consent)
        assertTrue(viewModel.uiState.value.connecting)
        assertTrue(sync.calls.isEmpty())

        viewModel.consentShown()
        assertNull(viewModel.uiState.value.consent)

        viewModel.onConsentResult(null)
        assertEquals(listOf("enabled:true"), sync.calls)
        assertFalse(viewModel.uiState.value.connecting)
    }

    @Test
    fun decliningTheConsentLeavesTheSyncOff() = runTest(mainDispatcherRule.testDispatcher) {
        authorizer.onAuthorize = DriveGrant.NeedsConsent(consent)
        authorizer.afterConsent = DriveGrant.Declined
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)
        viewModel.onConsentResult(null)

        assertTrue(sync.calls.isEmpty())
        assertFalse(viewModel.uiState.value.connecting)
        assertFalse(viewModel.uiState.value.connectionFailed)
    }

    @Test
    fun aPermissionDriveRejectsDoesNotTurnTheSyncOn() = runTest(mainDispatcherRule.testDispatcher) {
        cloud.failWith = SyncError.NotAuthorized()
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertTrue(sync.calls.isEmpty())
        assertTrue(viewModel.uiState.value.connectionFailed)
        assertFalse(viewModel.uiState.value.connecting)
    }

    @Test
    fun googleFailingIsShownAsAConnectionFailure() = runTest(mainDispatcherRule.testDispatcher) {
        authorizer.onAuthorize = DriveGrant.Failed()
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertTrue(viewModel.uiState.value.connectionFailed)
    }

    private fun problemAfter(grant: DriveGrant? = null, driveError: SyncError? = null): ConnectionProblem? {
        grant?.let { authorizer.onAuthorize = it }
        driveError?.let { cloud.failWith = it }
        return runBlocking {
            val viewModel = viewModel()
            val job = launch(Dispatchers.Unconfined) { viewModel.uiState.collect {} }
            viewModel.setEnabled(true)
            viewModel.uiState.value.connectionProblem.also { job.cancel() }
        }
    }

    @Test
    fun noPlayServicesIsToldApart() {
        assertEquals(ConnectionProblem.NO_PLAY_SERVICES, problemAfter(DriveGrant.Failed(FailureCause.NO_PLAY_SERVICES)))
    }

    @Test
    fun noNetworkIsToldApartWhetherGoogleOrDriveReportsIt() {
        assertEquals(ConnectionProblem.NO_NETWORK, problemAfter(DriveGrant.Failed(FailureCause.NO_NETWORK)))
    }

    @Test
    fun driveRefusingAnAcceptedPermissionMeansDriveIsUnavailableForTheAccount() {
        assertEquals(ConnectionProblem.DRIVE_UNAVAILABLE, problemAfter(driveError = SyncError.NotAuthorized()))
    }

    @Test
    fun aFullDriveIsToldApart() {
        assertEquals(ConnectionProblem.DRIVE_FULL, problemAfter(driveError = SyncError.QuotaExceeded()))
    }

    @Test
    fun anUnknownGoogleFailureKeepsTheGenericMessage() {
        assertEquals(ConnectionProblem.UNKNOWN, problemAfter(DriveGrant.Failed()))
    }

    @Test
    fun turningItOffStopsTheSync() = runTest(mainDispatcherRule.testDispatcher) {
        sync.status.value = SyncStatus.Synced(1)
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(false)

        assertEquals(listOf("enabled:false"), sync.calls)
        assertEquals(SyncStatus.Disabled, viewModel.uiState.value.status)
    }

    @Test
    fun theWifiOptionAndSyncNowGoToTheSync() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.setWifiOnly(false)
        viewModel.syncNow()

        assertEquals(listOf("wifiOnly:false", "syncNow"), sync.calls)
    }

    @Test
    fun theWifiOptionIsShownAsSaved() = runTest(mainDispatcherRule.testDispatcher) {
        settings.settings.value = SyncSettings(enabled = true, wifiOnly = false)
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        assertFalse(viewModel.uiState.value.wifiOnly)
    }

    @Test
    fun retryingATemporaryFailureJustSyncsAgain() = runTest(mainDispatcherRule.testDispatcher) {
        sync.status.value = SyncStatus.Failed(SyncError.NoInternet(java.io.IOException()))
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.retry()

        assertEquals(listOf("syncNow"), sync.calls)
        assertEquals(0, authorizer.authorizeCalls)
    }

    @Test
    fun retryingALostPermissionAsksForItAgain() = runTest(mainDispatcherRule.testDispatcher) {
        sync.status.value = SyncStatus.Failed(SyncError.NotAuthorized())
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.retry()

        assertEquals(1, authorizer.authorizeCalls)
        assertEquals(listOf("enabled:true"), sync.calls)
    }

    private fun libraryWith(vararg records: SyncRecord) = SyncManifest.encode(SyncSnapshot(records = records.toList()))

    private val someRecord = SyncRecord(RecordKey("book", "b1"), updatedAt = 1, data = "{}")

    @Test
    fun anAccountWithNothingInDriveIsKnownToHaveNoLibrary() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertEquals(false, viewModel.uiState.value.cloudHasData)
    }

    @Test
    fun aManifestWithoutRecordsIsNoLibraryEither() = runTest(mainDispatcherRule.testDispatcher) {
        cloud.otherDeviceWrites(libraryWith())
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertEquals(false, viewModel.uiState.value.cloudHasData)
    }

    @Test
    fun anAccountWhoseDriveHoldsRecordsIsKnownToHaveALibrary() = runTest(mainDispatcherRule.testDispatcher) {
        cloud.otherDeviceWrites(libraryWith(someRecord))
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertEquals(true, viewModel.uiState.value.cloudHasData)
    }

    @Test
    fun whenItCannotReadTheManifestItAssumesThereIsALibraryAndLetsTheSyncExplain() = runTest(mainDispatcherRule.testDispatcher) {
        cloud.otherDeviceWrites("not json")
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.setEnabled(true)

        assertEquals(true, viewModel.uiState.value.cloudHasData)
    }

    @Test
    fun beforeTheSyncIsTurnedOnItIsNotKnown() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        assertEquals(null, viewModel.uiState.value.cloudHasData)
    }
}
