package io.github.fabiann1809.reader.ui.account

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.sync.SyncControl
import io.github.fabiann1809.reader.data.sync.SyncManifest
import io.github.fabiann1809.reader.data.sync.SyncSettingsStore
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.auth.Consent
import io.github.fabiann1809.reader.data.sync.auth.DriveAuthorizer
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.auth.FailureCause
import io.github.fabiann1809.reader.data.sync.cloud.CloudStorage
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why connecting to Drive failed, so the screen can say what to do about it. */
enum class ConnectionProblem { NO_PLAY_SERVICES, NO_NETWORK, DRIVE_FULL, DRIVE_UNAVAILABLE, UNKNOWN }

data class SyncUiState(
    val status: SyncStatus = SyncStatus.Disabled,
    val wifiOnly: Boolean = true,
    // Asking Google for the permission and checking that Drive accepts it.
    val connecting: Boolean = false,
    val connectionProblem: ConnectionProblem? = null,
    // Set while Google's consent screen has to be opened; the screen clears it with [SyncViewModel.consentShown].
    val consent: Consent? = null,
    // Whether this account already has a library in Drive, known once the permission is given: null before.
    // The invitation after signing in (T18.9d) waits for the first sync only when there is something to bring.
    val cloudHasData: Boolean? = null,
) {
    val connectionFailed get() = connectionProblem != null
}

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

    private data class Connection(
        val connecting: Boolean = false,
        val problem: ConnectionProblem? = null,
        val consent: Consent? = null,
        val cloudHasData: Boolean? = null,
    )

    private val connection = MutableStateFlow(Connection())

    val uiState: StateFlow<SyncUiState> = combine(sync.status, settings.settings, connection) { status, saved, connection ->
        SyncUiState(status, saved.wifiOnly, connection.connecting, connection.problem, connection.consent, connection.cloudHasData)
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
            is DriveGrant.Granted -> {
                val drive = cloud.listFiles()
                if (drive.isSuccess) {
                    connection.value = Connection(cloudHasData = cloudHasLibrary())
                    sync.setEnabled(true)
                } else {
                    connection.value = Connection(problem = problemOf(drive.exceptionOrNull()))
                }
            }
            is DriveGrant.NeedsConsent -> connection.update { it.copy(consent = grant.consent) }
            DriveGrant.Declined -> connection.value = Connection()
            is DriveGrant.Failed -> connection.value = Connection(problem = problemOf(grant.cause))
        }
    }

    private fun problemOf(cause: FailureCause) = when (cause) {
        FailureCause.NO_PLAY_SERVICES -> ConnectionProblem.NO_PLAY_SERVICES
        FailureCause.NO_NETWORK -> ConnectionProblem.NO_NETWORK
        FailureCause.UNKNOWN -> ConnectionProblem.UNKNOWN
    }

    // Google gave the permission but Drive refused: typically an account whose organization turned Drive off.
    private fun problemOf(error: Throwable?) = when (error) {
        is SyncError.NoInternet -> ConnectionProblem.NO_NETWORK
        is SyncError.QuotaExceeded -> ConnectionProblem.DRIVE_FULL
        else -> ConnectionProblem.DRIVE_UNAVAILABLE
    }

    /** Whether Drive already holds records of this account. When it can't tell, it says yes: the sync will show why. */
    private suspend fun cloudHasLibrary(): Boolean {
        val manifest = cloud.readManifest().getOrElse { return true } ?: return false
        return try {
            SyncManifest.decode(manifest.content).records.isNotEmpty()
        } catch (e: SyncError) {
            true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
