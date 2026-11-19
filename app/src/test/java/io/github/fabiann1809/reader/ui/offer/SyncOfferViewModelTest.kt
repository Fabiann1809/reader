package io.github.fabiann1809.reader.ui.offer

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.data.sync.SyncSettings
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.FakeSyncSettingsStore
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SyncOfferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSyncSettingsStore()
    private val auth = FakeAuthRepository()

    private fun viewModel() = SyncOfferViewModel(auth, settings)

    private fun signIn(uid: String) = auth.signInAs(AuthUser(uid, "$uid@example.com"))

    @Test
    fun waitsWhileNobodyIsSignedIn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        assertEquals(OfferState.Checking, viewModel.state.value)
    }

    @Test
    fun anAccountThatWasNeverAskedIsOfferedTheSync() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("ana")

        assertEquals(OfferState.Offer, viewModel.state.value)
    }

    @Test
    fun withTheSyncAlreadyOnNothingIsAsked() = runTest(mainDispatcherRule.testDispatcher) {
        settings.settings.value = SyncSettings(enabled = true)
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("ana")

        assertEquals(OfferState.Done, viewModel.state.value)
    }

    @Test
    fun anAccountWaitingForItsLibraryIsNotAskedBecauseTheRestoreTurnsTheSyncOn() = runTest(mainDispatcherRule.testDispatcher) {
        settings.settings.value = SyncSettings(restoreFor = setOf("ana"))
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("ana")

        assertEquals(OfferState.Done, viewModel.state.value)
    }

    @Test
    fun anAccountAlreadyAskedIsNotAskedAgain() = runTest(mainDispatcherRule.testDispatcher) {
        settings.settings.value = SyncSettings(offeredTo = setOf("ana"))
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)

        signIn("ana")

        assertEquals(OfferState.Done, viewModel.state.value)
    }

    @Test
    fun answeringRemembersTheAccountAndOpensTheApp() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)
        signIn("ana")

        viewModel.answered()

        assertEquals(OfferState.Done, viewModel.state.value)
        assertEquals(setOf("ana"), settings.settings.value.offeredTo)
    }

    @Test
    fun eachAccountIsAskedOnItsOwn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.state.launchIn(backgroundScope)
        signIn("ana")
        viewModel.answered()

        auth.signOut()
        assertEquals(OfferState.Checking, viewModel.state.value)
        signIn("beto")

        assertEquals(OfferState.Offer, viewModel.state.value)
        assertEquals(setOf("ana"), settings.settings.value.offeredTo)
    }
}
