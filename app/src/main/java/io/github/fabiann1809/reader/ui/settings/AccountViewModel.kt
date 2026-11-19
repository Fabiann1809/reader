package io.github.fabiann1809.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.sync.SyncControl
import io.github.fabiann1809.reader.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    // With the sync on, signing out first saves everything to Drive and empties the phone.
    val syncOn: Boolean = false,
    val signingOut: Boolean = false,
    // Saving to Drive failed, so the session was kept to lose nothing.
    val saveFailed: Boolean = false,
)

/** The "Cuenta" block of Ajustes (T22.5): who is signed in, and the way out (T18.8: which saves to Drive first). */
class AccountViewModel(private val auth: AuthRepository, private val sync: SyncControl) : ViewModel() {

    /** The signed-in email; null for an account without one. */
    val email: StateFlow<String?> = auth.currentUser.map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), auth.currentUser.value?.email)

    private val signOutState = MutableStateFlow(AccountUiState())

    val uiState: StateFlow<AccountUiState> = combine(sync.status, signOutState) { status, state ->
        state.copy(syncOn = status != SyncStatus.Disabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AccountUiState())

    /**
     * Signing out brings the sign-in screen back, because the app root watches the session. With the sync
     * on, the data is saved to Drive first; if that fails the session stays, so nothing is lost.
     */
    fun signOut() {
        if (signOutState.value.signingOut) return
        signOutState.update { it.copy(signingOut = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = sync.saveBeforeSignOut()
            if (saved.isSuccess) auth.signOut()
            signOutState.update { it.copy(signingOut = false, saveFailed = saved.isFailure) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
