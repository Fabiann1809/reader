package io.github.fabiann1809.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The "Cuenta" block of Ajustes (T22.5): who is signed in, and the way out. */
class AccountViewModel(private val auth: AuthRepository) : ViewModel() {

    /** The signed-in email; null for an account without one. */
    val email: StateFlow<String?> = auth.currentUser.map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), auth.currentUser.value?.email)

    /** Signing out brings the sign-in screen back, because the app root watches the session. */
    fun signOut() = auth.signOut()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
