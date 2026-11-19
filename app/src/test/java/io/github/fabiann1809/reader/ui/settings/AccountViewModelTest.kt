package io.github.fabiann1809.reader.ui.settings

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.FakeSyncControl
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(AuthUser("uid", "ana@example.com"))
    private val sync = FakeSyncControl()

    private fun viewModel() = AccountViewModel(auth, sync)

    @Test
    fun showsTheEmailOfThePersonSignedIn() {
        assertEquals("ana@example.com", viewModel().email.value)
    }

    @Test
    fun signingOutEndsTheSession() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.email.launchIn(backgroundScope)

        viewModel.signOut()

        assertNull(auth.currentUser.value)
        assertNull(viewModel.email.value)
    }

    @Test
    fun theSessionEndsOnlyAfterSavingToTheCloud() = runTest(mainDispatcherRule.testDispatcher) {
        sync.status.value = SyncStatus.Synced(1)
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)
        assertTrue(viewModel.uiState.value.syncOn)

        viewModel.signOut()

        assertEquals(listOf("saveBeforeSignOut"), sync.calls)
        assertNull(auth.currentUser.value)
        assertFalse(viewModel.uiState.value.signingOut)
    }

    @Test
    fun ifSavingFailsTheSessionStaysAndTheFailureIsShown() = runTest(mainDispatcherRule.testDispatcher) {
        sync.status.value = SyncStatus.Synced(1)
        sync.saveResult = Result.failure(SyncError.NoInternet(java.io.IOException()))
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.signOut()

        assertNotNull(auth.currentUser.value)
        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.signingOut)
    }

    @Test
    fun tryingAgainClearsTheFailure() = runTest(mainDispatcherRule.testDispatcher) {
        sync.status.value = SyncStatus.Synced(1)
        sync.saveResult = Result.failure(SyncError.Unknown("x"))
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)
        viewModel.signOut()

        sync.saveResult = Result.success(Unit)
        viewModel.signOut()

        assertFalse(viewModel.uiState.value.saveFailed)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun withTheSyncOffThereIsNothingToConfirm() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        assertFalse(viewModel.uiState.value.syncOn)
    }
}
