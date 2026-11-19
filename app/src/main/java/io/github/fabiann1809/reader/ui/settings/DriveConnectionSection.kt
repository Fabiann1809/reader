package io.github.fabiann1809.reader.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.OutlineButton

/** "Sincronización": the connection to Google Drive (T18.4); T18.6 adds the sync controls. */
@Composable
fun DriveConnectionSection(state: DriveConnectionUiState, viewModel: DriveConnectionViewModel) {
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onConsentResult(it.data)
    }
    LaunchedEffect(state.consent) {
        val consent = state.consent ?: return@LaunchedEffect
        consentLauncher.launch(consent.toIntentSenderRequest())
        viewModel.consentShown()
    }
    SettingsSection(R.string.settings_sync) {
        Text(
            text = stringResource(R.string.settings_sync_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val status = when (state.status) {
            DriveStatus.DISCONNECTED -> null
            DriveStatus.CONNECTING -> R.string.settings_sync_connecting
            DriveStatus.CONNECTED -> R.string.settings_sync_connected
            DriveStatus.FAILED -> R.string.settings_sync_failed
        }
        if (status != null) {
            Text(
                text = stringResource(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlineButton(
            text = stringResource(R.string.settings_sync_connect),
            onClick = viewModel::connect,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.status != DriveStatus.CONNECTING,
        )
    }
}
