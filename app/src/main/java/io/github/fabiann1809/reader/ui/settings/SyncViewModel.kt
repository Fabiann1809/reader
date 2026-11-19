package io.github.fabiann1809.reader.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.sync.SyncControl
import io.github.fabiann1809.reader.data.sync.SyncSettingsStore
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.auth.Consent
import io.github.fabiann1809.reader.data.sync.auth.DriveAuthorizer
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.cloud.CloudStorage
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val status: SyncStatus = SyncStatus.Disabled,
    val wifiOnly: Boolean = true,
    // Asking Google for the permission and checking that Drive accepts it.
    val connecting: Boolean = false,
    val connectionFailed: Boolean = false,
    // Set while Google's consent screen has to be opened; the screen clears it with [SyncViewModel.consentShown].
    val consent: Consent? = null,
)

/**
 * The "Sincronización" block of Ajustes (T18.7). Turning it on first asks for the Drive permission
 * and checks it works; then the sync takes over and this only shows how it is going.
 */
class SyncViewModel(
    private val authorizer: DriveAuthorizer,
    private val cloud: CloudStorage,
    private val sync: SyncControl,
    settings: SyncSettingsStore,
) : ViewModel() {

    private data class Connection(val connecting: Boolean = false, val failed: Boolean = false, val consent: Consent? = null)

    private val connection = MutableStateFlow(Connection())

    val uiState: StateFlow<SyncUiState> = combine(sync.status, settings.settings, connection) { status, saved, connection ->
        SyncUiState(status, saved.wifiOnly, connection.connecting, connection.failed, connection.consent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SyncUiState())

    fun setEnabled(enabled: Boolean) {
        if (enabled) connect() else viewModelScope.launch { sync.setEnabled(false) }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch { sync.setWifiOnly(wifiOnly) }
    }

    fun syncNow() {
        viewModelScope.launch { sync.syncNow() }
    }

    /** "Reintentar": a lost permission has to be asked for again; anything else just tries again. */
    fun retry() {
        val failure = (uiState.value.status as? SyncStatus.Failed)?.error
        if (failure is SyncError.NotAuthorized) connect() else syncNow()
    }

    /** The consent screen closed; [data] is what it returned. */
    fun onConsentResult(data: Intent?) {
        viewModelScope.launch { handle(authorizer.grantFrom(data)) }
    }

    fun consentShown() = connection.update { it.copy(consent = null) }

    private fun connect() {
        connection.value = Connection(connecting = true)
        viewModelScope.launch { handle(authorizer.authorize()) }
    }

    private suspend fun handle(grant: DriveGrant) {
        when (grant) {
            // A token alone proves nothing: listing the folder shows that Drive accepts it.
            is DriveGrant.Granted -> if (cloud.listFiles().isSuccess) {
                connection.value = Connection()
                sync.setEnabled(true)
            } else {
                connection.value = Connection(failed = true)
            }
            is DriveGrant.NeedsConsent -> connection.update { it.copy(consent = grant.consent) }
            DriveGrant.Declined -> connection.value = Connection()
            DriveGrant.Failed -> connection.value = Connection(failed = true)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
