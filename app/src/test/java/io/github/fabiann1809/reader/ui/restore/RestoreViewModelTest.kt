package io.github.fabiann1809.reader.ui.restore

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.data.sync.SyncRestorer
import io.github.fabiann1809.reader.data.sync.SyncSettings
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import io.github.fabiann1809.reader.testing.FakeDriveAuthorizer
import io.github.fabiann1809.reader.testing.FakeLocalChangeSource
import io.github.fabiann1809.reader.testing.FakeSyncControl
import io.github.fabiann1809.reader.testing.FakeSyncSettingsStore
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RestoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSyncSettingsStore(SyncSettings(restoreFor = setOf("ana")))
    private val sync = FakeSyncControl().apply { statusAfterEnable = SyncStatus.Synced(5_000) }
    private val changes = FakeLocalChangeSource()
    private val authorizer = FakeDriveAuthorizer()
    private val auth = FakeAuthRepository()

    private fun viewModel(): RestoreViewModel {
        val restorer = SyncRestorer(settings, authorizer, FakeCloudStorage(), sync, changes)
        return RestoreViewModel(auth, restorer)
    }

    private fun signIn(uid: String) {
        auth.signInAs(AuthUser(uid, "$uid@example.com"))
    }

    @Test
    fun nothingToDoWhileNobodyIsSignedIn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        assertEquals(RestoreState.Checking, viewModel.state.value)
    }

    @Test
    fun theAccountThatSignedOutGetsItsLibraryBackBeforeTheAppOpens() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("ana")

        assertEquals(RestoreState.Done, viewModel.state.value)
        assertEquals(listOf("enabled:true"), sync.calls)
        assertTrue(settings.settings.value.restoreFor.isEmpty())
    }

    @Test
    fun anotherAccountOpensTheAppWithoutRestoringAnything() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("beto")

        assertEquals(RestoreState.Done, viewModel.state.value)
        assertTrue(sync.calls.isEmpty())
    }

    @Test
    fun aFailureShowsWhyAndRetryingCanSucceed() = runTest(mainDispatcherRule.testDispatcher) {
        sync.statusAfterEnable = SyncStatus.Failed(SyncError.NoInternet(java.io.IOException()))
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("ana")

        assertTrue((viewModel.state.value as RestoreState.Failed).error is SyncError.NoInternet)

        sync.statusAfterEnable = SyncStatus.Synced(6_000)
        viewModel.retry()

        assertEquals(RestoreState.Done, viewModel.state.value)
    }

    @Test
    fun goingOnWithoutWaitingOpensTheAppAndStopsExpectingTheRestore() = runTest(mainDispatcherRule.testDispatcher) {
        // The first sync never ends (waiting for Wi-Fi, say): the status stays as the "on, nothing yet" one.
        sync.statusAfterEnable = SyncStatus.Pending
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)
        signIn("ana")
        assertEquals(RestoreState.Restoring, viewModel.state.value)

        viewModel.continueWithout()

        assertEquals(RestoreState.Done, viewModel.state.value)
        assertTrue(settings.settings.value.restoreFor.isEmpty())
    }

    @Test
    fun signingOutAndInAgainChecksAgain() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)
        signIn("ana")
        assertEquals(RestoreState.Done, viewModel.state.value)

        auth.signOut()
        assertEquals(RestoreState.Checking, viewModel.state.value)

        // She signed out again with the sync on, so her library waits once more.
        settings.settings.value = SyncSettings(restoreFor = setOf("ana"))
        sync.calls.clear()
        signIn("ana")

        assertEquals(RestoreState.Done, viewModel.state.value)
        assertEquals(listOf("enabled:true"), sync.calls)
    }
}
