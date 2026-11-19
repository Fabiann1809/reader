package io.github.fabiann1809.reader.ui.settings

import io.github.fabiann1809.reader.data.sync.auth.Consent
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import io.github.fabiann1809.reader.testing.FakeDriveAuthorizer
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DriveConnectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authorizer = FakeDriveAuthorizer()
    private val cloud = FakeCloudStorage()
    private val consent = Consent { error("Not opened in unit tests") }

    private fun viewModel() = DriveConnectionViewModel(authorizer, cloud)

    @Test
    fun startsDisconnected() {
        assertEquals(DriveStatus.DISCONNECTED, viewModel().uiState.value.status)
    }

    @Test
    fun aGrantedPermissionThatDriveAcceptsConnects() {
        val viewModel = viewModel()

        viewModel.connect()

        assertEquals(DriveStatus.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun aTokenDriveRejectsDoesNotConnect() {
        cloud.failWith = SyncError.NotAuthorized()
        val viewModel = viewModel()

        viewModel.connect()

        assertEquals(DriveStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun theFirstTimeAsksForConsentAndConnectsAfterTheUserAccepts() {
        authorizer.onAuthorize = DriveGrant.NeedsConsent(consent)
        val viewModel = viewModel()

        viewModel.connect()

        assertEquals(consent, viewModel.uiState.value.consent)
        assertEquals(DriveStatus.CONNECTING, viewModel.uiState.value.status)

        viewModel.consentShown()
        assertNull(viewModel.uiState.value.consent)

        viewModel.onConsentResult(null)
        assertEquals(DriveStatus.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun decliningLeavesItDisconnected() {
        authorizer.onAuthorize = DriveGrant.NeedsConsent(consent)
        authorizer.afterConsent = DriveGrant.Declined
        val viewModel = viewModel()

        viewModel.connect()
        viewModel.onConsentResult(null)

        assertEquals(DriveStatus.DISCONNECTED, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.consent)
    }

    @Test
    fun googleFailingIsShownAsAFailure() {
        authorizer.onAuthorize = DriveGrant.Failed
        val viewModel = viewModel()

        viewModel.connect()

        assertEquals(DriveStatus.FAILED, viewModel.uiState.value.status)
    }

    @Test
    fun tryingAgainAfterAFailureCanConnect() {
        authorizer.onAuthorize = DriveGrant.Failed
        val viewModel = viewModel()
        viewModel.connect()

        authorizer.onAuthorize = DriveGrant.Granted("token")
        viewModel.connect()

        assertEquals(DriveStatus.CONNECTED, viewModel.uiState.value.status)
        assertEquals(2, authorizer.authorizeCalls)
        assertNotNull(viewModel.uiState.value)
    }
}
