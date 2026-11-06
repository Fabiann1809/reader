package io.github.fabiann1809.reader.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthError
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.auth.EmailValidator
import io.github.fabiann1809.reader.data.auth.PasswordPolicy
import io.github.fabiann1809.reader.data.auth.PasswordRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

/** A problem with one field, shown under it. */
enum class FieldError {
    EMAIL_REQUIRED, EMAIL_INVALID, EMAIL_IN_USE,
    PASSWORD_REQUIRED, PASSWORD_WEAK,
    CONFIRM_REQUIRED, CONFIRM_MISMATCH,
}

/** A requirement of the new password and whether what is typed meets it yet. */
data class PasswordCheck(val rule: PasswordRule, val passed: Boolean)

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val mode: AuthMode = AuthMode.SIGN_IN,
    val loading: Boolean = false,
    /** What the service answered. Part of it belongs under a field, see the *Error properties. */
    val error: AuthError? = null,
    val resetSent: Boolean = false,
    /** Set when the button is pressed: until then an empty field is not yet a mistake. */
    val submitted: Boolean = false,
) {
    private val signingUp get() = mode == AuthMode.SIGN_UP

    /** Nothing is ticked before the person starts typing. */
    val passwordChecks: List<PasswordCheck> = PasswordRule.entries.map {
        PasswordCheck(it, passed = password.isNotEmpty() && PasswordPolicy.passes(it, password))
    }

    val emailError: FieldError? = when {
        error is AuthError.EmailAlreadyInUse -> FieldError.EMAIL_IN_USE
        error is AuthError.InvalidEmail -> FieldError.EMAIL_INVALID
        !submitted -> null
        email.isBlank() -> FieldError.EMAIL_REQUIRED
        !EmailValidator.isValid(email) -> FieldError.EMAIL_INVALID
        else -> null
    }

    val passwordError: FieldError? = when {
        error is AuthError.WeakPassword -> FieldError.PASSWORD_WEAK
        !submitted -> null
        password.isEmpty() -> FieldError.PASSWORD_REQUIRED
        signingUp && !PasswordPolicy.isValid(password) -> FieldError.PASSWORD_WEAK
        else -> null
    }

    // The mismatch shows while typing, once there is something to compare.
    val confirmError: FieldError? = when {
        !signingUp -> null
        confirmPassword.isNotEmpty() && confirmPassword != password -> FieldError.CONFIRM_MISMATCH
        submitted && confirmPassword.isEmpty() -> FieldError.CONFIRM_REQUIRED
        else -> null
    }

    /** What is left for the message above the button: the errors that belong to no field. */
    val generalError: AuthError? = error.takeUnless {
        it is AuthError.EmailAlreadyInUse || it is AuthError.InvalidEmail || it is AuthError.WeakPassword
    }

    val hasFieldErrors: Boolean get() = emailError != null || passwordError != null || confirmError != null
}

/**
 * The sign-in screen (T22.3). A successful call changes [AuthRepository.currentUser], which is what
 * moves the app on, so there is nothing to navigate from here.
 */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null, resetSent = false) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, error = null) }

    fun onConfirmPasswordChange(confirm: String) = _uiState.update { it.copy(confirmPassword = confirm, error = null) }

    fun switchMode() = _uiState.update {
        val other = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
        it.copy(mode = other, confirmPassword = "", error = null, resetSent = false, submitted = false)
    }

    fun submit() {
        _uiState.update { it.copy(submitted = true) }
        val state = _uiState.value
        if (state.hasFieldErrors) return
        launchCall {
            if (state.mode == AuthMode.SIGN_UP) auth.signUp(state.email, state.password) else auth.signIn(state.email, state.password)
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        if (!EmailValidator.isValid(email)) {
            _uiState.update { it.copy(error = AuthError.InvalidEmail(), resetSent = false) }
            return
        }
        launchCall {
            auth.sendPasswordReset(email)
            _uiState.update { it.copy(resetSent = true) }
        }
    }

    /** [getIdToken] opens the Google account picker; it is a parameter so tests need no Android. */
    fun signInWithGoogle(getIdToken: suspend () -> String) = launchCall { auth.signInWithGoogle(getIdToken()) }

    private fun launchCall(call: suspend () -> Unit) {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, error = null, resetSent = false) }
        viewModelScope.launch {
            val error = try {
                call()
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthError) {
                e
            } catch (e: Exception) {
                AuthError.Unknown(e)
            }
            // Closing the Google picker is not a failure worth a message.
            _uiState.update { state -> state.copy(loading = false, error = error.takeUnless { it is AuthError.Cancelled }) }
        }
    }
}
