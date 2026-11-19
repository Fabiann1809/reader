package io.github.fabiann1809.reader.ui.offer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.account.SyncConsentEffect
import io.github.fabiann1809.reader.ui.account.SyncUiState
import io.github.fabiann1809.reader.ui.account.SyncViewModel
import io.github.fabiann1809.reader.ui.auth.ReaderLogo
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.restore.RestoreScreen
import io.github.fabiann1809.reader.ui.restore.RestoreState

/**
 * The invitation to sync with Drive shown once after signing in (T18.9d). When it is accepted the loading
 * screen takes over until the first sync ends, so the library never shows empty while the books come in.
 * [onAnswered] ends it, yes or no.
 */
@Composable
fun SyncOfferScreen(onAnswered: () -> Unit, sync: SyncViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by sync.uiState.collectAsStateWithLifecycle()
    SyncConsentEffect(state, sync)
    // The first sync ended: the library is there. Or there was nothing in Drive to wait for: a new account
    // goes straight to its empty library, and what it adds from then on is synced in the background.
    LaunchedEffect(state.status, state.cloudHasData) {
        val status = state.status
        val ended = status is SyncStatus.Synced && status.at != null
        val nothingToBring = status != SyncStatus.Disabled && state.cloudHasData == false
        if (ended || nothingToBring) onAnswered()
    }
    SyncOfferContent(state, onEnable = { sync.setEnabled(true) }, onSkip = onAnswered, onRetry = sync::retry)
}

@Composable
fun SyncOfferContent(state: SyncUiState, onEnable: () -> Unit, onSkip: () -> Unit, onRetry: () -> Unit = {}) {
    // Turned on: wait for the first sync with the same loading screen as a returning account's restore.
    val status = state.status
    if (status != SyncStatus.Disabled) {
        // Nothing in Drive: the screen is about to close, so it shows nothing rather than a loading that isn't true.
        if (state.cloudHasData == false) return
        val loading = if (status is SyncStatus.Failed) RestoreState.Failed(status.error) else RestoreState.Restoring
        RestoreScreen(loading, onContinue = onSkip, onRetry = onRetry)
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        ReaderLogo(iconSize = 60.dp)
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(R.string.offer_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.offer_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.offer_later_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.connectionFailed) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_sync_connect_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = stringResource(if (state.connecting) R.string.settings_sync_connecting else R.string.offer_enable),
            onClick = onEnable,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.connecting,
            loading = state.connecting,
        )
        TextButton(onClick = onSkip, enabled = !state.connecting) { Text(stringResource(R.string.offer_skip)) }
    }
}
