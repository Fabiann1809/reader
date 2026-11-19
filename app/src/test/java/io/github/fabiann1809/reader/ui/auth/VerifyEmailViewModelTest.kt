package io.github.fabiann1809.reader.ui.auth

import io.github.fabiann1809.reader.data.auth.AuthError
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VerifyEmailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()

    // Creating the account is what leaves the person signed in with an unverified email.
    private suspend fun viewModel(): VerifyEmailViewModel {
        auth.signUp("ana@example.com", "Secreta123")
        return VerifyEmailViewModel(auth)
    }

    @Test
    fun showsTheAddressTheLinkWasSentTo() = runTest(mainDispatcherRule.testDispatcher) {
        assertEquals("ana@example.com", viewModel().uiState.value.email)
    }

    @Test
    fun creatingTheAccountSendsTheLinkOnce() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel()

        assertEquals(listOf("ana@example.com"), auth.verificationEmails)
    }

    @Test
    fun anAccountStaysUnverifiedUntilTheLinkIsOpened() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.check(byPerson = true)

        assertFalse(auth.currentUser.value!!.emailVerified)
        assertTrue(viewModel.uiState.value.stillPending)
    }

    @Test
    fun theAutomaticCheckStaysQuietWhenNothingChanged() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.check(byPerson = false)

        assertFalse(viewModel.uiState.value.stillPending)
    }

    @Test
    fun openingTheLinkAndCheckingVerifiesTheAccount() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        auth.openVerificationLink("ana@example.com")

        viewModel.check(byPerson = true)

        assertTrue(auth.currentUser.value!!.emailVerified)
        assertFalse(viewModel.uiState.value.stillPending)
    }

    @Test
    fun resendingSendsAnotherLinkAndSaysSo() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.resend()

        assertEquals(2, auth.verificationEmails.size)
        assertTrue(viewModel.uiState.value.resent)
    }

    @Test
    fun aFailureIsShownAndTheButtonsWorkAgain() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        auth.failWith = AuthError.TooManyAttempts()

        viewModel.resend()

        assertTrue(viewModel.uiState.value.error is AuthError.TooManyAttempts)
        assertFalse(viewModel.uiState.value.working)
        assertFalse(viewModel.uiState.value.resent)
    }

    @Test
    fun usingAnotherEmailLeavesTheAccount() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.useAnotherEmail()

        assertNull(auth.currentUser.value)
    }
}
