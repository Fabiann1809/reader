package io.github.fabiann1809.reader.ui.auth

import io.github.fabiann1809.reader.data.auth.AuthError
import io.github.fabiann1809.reader.data.auth.PasswordRule
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = AuthViewModel(auth)
    private val state get() = viewModel.uiState.value

    private fun fill(email: String = "ana@example.com", password: String = GOOD_PASSWORD, confirm: String = password) {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmPasswordChange(confirm)
    }

    private fun createAccount() {
        viewModel.switchMode()
        fill()
        viewModel.submit()
    }

    @Test
    fun creatingAnAccountSignsThePersonIn() {
        createAccount()

        assertEquals("ana@example.com", auth.currentUser.value?.email)
        assertFalse(state.loading)
        assertNull(state.error)
        assertFalse(state.hasFieldErrors)
    }

    @Test
    fun anExistingAccountSignsIn() {
        createAccount()
        auth.signOut()
        viewModel.switchMode()
        viewModel.onPasswordChange(GOOD_PASSWORD)

        viewModel.submit()

        assertNotNull(auth.currentUser.value)
    }

    @Test
    fun aWrongPasswordIsAGeneralErrorAndStaysSignedOut() {
        createAccount()
        auth.signOut()
        viewModel.switchMode()
        viewModel.onPasswordChange("other-password")

        viewModel.submit()

        assertTrue(state.generalError is AuthError.WrongCredentials)
        assertFalse(state.hasFieldErrors)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun nothingIsAMistakeBeforeThePersonPressesTheButton() {
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmError)
    }

    @Test
    fun emptyFieldsAreReportedTogetherAfterPressing() {
        viewModel.switchMode()

        viewModel.submit()

        assertEquals(FieldError.EMAIL_REQUIRED, state.emailError)
        assertEquals(FieldError.PASSWORD_REQUIRED, state.passwordError)
        assertEquals(FieldError.CONFIRM_REQUIRED, state.confirmError)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun theWeakPasswordFromTheReportIsStoppedBeforeTheService() {
        viewModel.switchMode()
        fill(password = "123456")

        viewModel.submit()

        assertEquals(FieldError.PASSWORD_WEAK, state.passwordError)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun everyRuleOfThePolicyBlocksTheSignUp() {
        viewModel.switchMode()
        listOf("Ab1#", "lectura#2026", "LECTURA#2026", "Lectura#abcd", "Lectura2026").forEach { weak ->
            fill(password = weak)
            viewModel.submit()
            assertEquals(weak, FieldError.PASSWORD_WEAK, state.passwordError)
        }
        assertNull(auth.currentUser.value)
    }

    @Test
    fun differentPasswordsSayTheyDoNotMatchEvenIfTheFirstIsWeak() {
        viewModel.switchMode()
        fill(password = "abc", confirm = "xyz")

        viewModel.submit()

        // Each problem is under its own field.
        assertEquals(FieldError.PASSWORD_WEAK, state.passwordError)
        assertEquals(FieldError.CONFIRM_MISMATCH, state.confirmError)
    }

    @Test
    fun theMismatchShowsWhileTypingAndGoesWhenTheyMatch() {
        viewModel.switchMode()
        viewModel.onPasswordChange(GOOD_PASSWORD)

        viewModel.onConfirmPasswordChange("Lectura")
        assertEquals(FieldError.CONFIRM_MISMATCH, state.confirmError)

        viewModel.onConfirmPasswordChange(GOOD_PASSWORD)
        assertNull(state.confirmError)
    }

    @Test
    fun theChecklistTicksEachRuleAsItIsMet() {
        viewModel.switchMode()
        assertTrue(state.passwordChecks.none { it.passed })

        viewModel.onPasswordChange("lectura1")
        val checks = state.passwordChecks.associate { it.rule to it.passed }
        assertTrue(checks.getValue(PasswordRule.MIN_LENGTH))
        assertTrue(checks.getValue(PasswordRule.HAS_LOWERCASE))
        assertTrue(checks.getValue(PasswordRule.HAS_DIGIT))
        assertFalse(checks.getValue(PasswordRule.HAS_UPPERCASE))
        assertFalse(checks.getValue(PasswordRule.HAS_SYMBOL))

        viewModel.onPasswordChange(GOOD_PASSWORD)
        assertTrue(state.passwordChecks.all { it.passed })
    }

    @Test
    fun badEmailsAreCaughtBeforeAskingTheService() {
        listOf("not-an-email", "ana@example", "ana perez@example.com", "ana..x@example.com").forEach { email ->
            fill(email = email)
            viewModel.submit()
            assertEquals(email, FieldError.EMAIL_INVALID, state.emailError)
        }
        assertNull(auth.currentUser.value)
    }

    @Test
    fun anEmailAlreadyInUseIsShownUnderTheEmail() {
        createAccount()
        auth.signOut()

        // Still in "Crear cuenta": the same email again.
        viewModel.submit()

        assertEquals(FieldError.EMAIL_IN_USE, state.emailError)
        assertNull(state.generalError)
    }

    @Test
    fun typingClearsTheServiceError() {
        auth.failWith = AuthError.TooManyAttempts()
        fill()
        viewModel.submit()
        assertTrue(state.generalError is AuthError.TooManyAttempts)

        viewModel.onEmailChange("ana@example.com")

        assertNull(state.error)
    }

    @Test
    fun switchingModeClearsTheConfirmationAndTheMistakes() {
        viewModel.switchMode()
        fill()
        viewModel.submit()

        viewModel.switchMode()

        assertEquals("", state.confirmPassword)
        assertFalse(state.submitted)
    }

    @Test
    fun googleSignsInWithTheTokenFromThePicker() {
        viewModel.signInWithGoogle { "token-123" }

        assertEquals("google-token-123", auth.currentUser.value?.uid)
    }

    @Test
    fun closingTheGooglePickerShowsNoError() {
        viewModel.signInWithGoogle { throw AuthError.Cancelled() }

        assertNull(state.error)
        assertFalse(state.loading)
    }

    @Test
    fun aMissingGoogleAccountIsTold() {
        viewModel.signInWithGoogle { throw AuthError.NoGoogleAccount() }

        assertTrue(state.generalError is AuthError.NoGoogleAccount)
    }

    @Test
    fun theResetEmailIsSentToTheTypedAddress() {
        viewModel.onEmailChange("ana@example.com")

        viewModel.resetPassword()

        assertEquals(listOf("ana@example.com"), auth.resetEmails)
        assertTrue(state.resetSent)
    }

    @Test
    fun resettingNeedsAValidEmailShownUnderTheField() {
        viewModel.resetPassword()

        assertEquals(FieldError.EMAIL_INVALID, state.emailError)
        assertTrue(auth.resetEmails.isEmpty())
    }

    private companion object {
        const val GOOD_PASSWORD = "Lectura#2026"
    }
}
