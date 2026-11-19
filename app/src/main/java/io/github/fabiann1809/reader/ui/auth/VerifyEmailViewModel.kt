package io.github.fabiann1809.reader.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthError
import io.github.fabiann1809.reader.data.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyEmailUiState(
    val email: String = "",
    val working: Boolean = false,
    /** The link was sent again. */
    val resent: Boolean = false,
    /** The person said they confirmed it, but the service still doesn't see it. */
    val stillPending: Boolean = false,
    val error: AuthError? = null,
)

/**
 * The "confirm your email" step after creating an account with an email address (T22.10). Nothing else of the
 * app opens until the link is opened; once it is, [AuthRepository.currentUser] says so and the app root moves on.
 */
class VerifyEmailViewModel(private val auth: AuthRepository) : ViewModel() {

    private val progress = MutableStateFlow(VerifyEmailUiState())

    val uiState: StateFlow<VerifyEmailUiState> = progress.asStateFlow()

    /** Checks whether the link was opened. [byPerson] is false for the automatic check on coming back to the app. */
    fun check(byPerson: Boolean) = run {
        auth.refreshUser()
        // If it is verified, the root screen changes and this one goes away; otherwise say so, but only when asked.
        if (byPerson && auth.currentUser.value?.emailVerified == false) progress.update { it.copy(stillPending = true) }
    }

    fun resend() = run {
        auth.sendEmailVerification()
        progress.update { it.copy(resent = true) }
    }

    /** For a mistyped or invented address: leaves this account so another can be used. */
    fun useAnotherEmail() = auth.signOut()

    private fun run(call: suspend () -> Unit) {
        if (progress.value.working) return
        progress.update { it.copy(working = true, error = null, resent = false, stillPending = false) }
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
            progress.update { it.copy(working = false, error = error) }
        }
    }

    init {
        progress.update { it.copy(email = auth.currentUser.value?.email.orEmpty()) }
    }
}
