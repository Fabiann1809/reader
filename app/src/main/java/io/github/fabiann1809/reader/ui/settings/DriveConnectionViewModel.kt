package io.github.fabiann1809.reader.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.sync.auth.Consent
import io.github.fabiann1809.reader.data.sync.auth.DriveAuthorizer
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.cloud.CloudStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DriveStatus { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

data class DriveConnectionUiState(
    val status: DriveStatus = DriveStatus.DISCONNECTED,
    // Set while Google's consent screen has to be opened; the screen clears it with [DriveConnectionViewModel.consentShown].
    val consent: Consent? = null,
)

/**
 * Connects the app to the user's Drive (T18.4): asks for the permission and checks that it really
 * opens the hidden folder. Ajustes (T18.6) builds the rest of the sync controls around it.
 */
class DriveConnectionViewModel(
    private val authorizer: DriveAuthorizer,
    private val cloud: CloudStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveConnectionUiState())
    val uiState: StateFlow<DriveConnectionUiState> = _uiState.asStateFlow()

    fun connect() {
        _uiState.update { it.copy(status = DriveStatus.CONNECTING) }
        viewModelScope.launch { handle(authorizer.authorize()) }
    }

    /** The consent screen closed; [data] is what it returned. */
    fun onConsentResult(data: Intent?) {
        viewModelScope.launch { handle(authorizer.grantFrom(data)) }
    }

    fun consentShown() = _uiState.update { it.copy(consent = null) }

    private suspend fun handle(grant: DriveGrant) {
        when (grant) {
            is DriveGrant.Granted -> verify()
            is DriveGrant.NeedsConsent -> _uiState.update { it.copy(consent = grant.consent) }
            DriveGrant.Declined -> setStatus(DriveStatus.DISCONNECTED)
            DriveGrant.Failed -> setStatus(DriveStatus.FAILED)
        }
    }

    /** A token alone proves nothing: listing the folder shows that Drive accepts it. */
    private suspend fun verify() {
        setStatus(if (cloud.listFiles().isSuccess) DriveStatus.CONNECTED else DriveStatus.FAILED)
    }

    private fun setStatus(status: DriveStatus) = _uiState.update { it.copy(status = status, consent = null) }
}
