package io.github.fabiann1809.reader.ui.account

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.settings.SettingsSection
import io.github.fabiann1809.reader.ui.settings.Toggle
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.ui.components.OutlineButton

/** "Sincronización": connect Google Drive, see how the sync is going, and ask for it now (T18.7). */
@Composable
fun SyncSection(state: SyncUiState, viewModel: SyncViewModel) {
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onConsentResult(it.data)
    }
    LaunchedEffect(state.consent) {
        val consent = state.consent ?: return@LaunchedEffect
        consentLauncher.launch(consent.toIntentSenderRequest())
        viewModel.consentShown()
    }
    SyncSectionContent(
        state = state,
        onEnabled = viewModel::setEnabled,
        onWifiOnly = viewModel::setWifiOnly,
        onSyncNow = viewModel::syncNow,
        onRetry = viewModel::retry,
    )
}

@Composable
fun SyncSectionContent(
    state: SyncUiState,
    onEnabled: (Boolean) -> Unit,
    onWifiOnly: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onRetry: () -> Unit,
) {
    val on = state.status != SyncStatus.Disabled
    SettingsSection(R.string.settings_sync) {
        Toggle(R.string.settings_sync_toggle, R.string.settings_sync_toggle_hint, checked = on || state.connecting) {
            // Pressing it while the permission is being asked for would only start a second request.
            if (!state.connecting) onEnabled(it)
        }
        if (state.connecting) SyncText(R.string.settings_sync_connecting)
        if (state.connectionFailed) SyncText(R.string.settings_sync_connect_failed, error = true)
        if (on) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusLine(state.status)
            Toggle(R.string.settings_sync_wifi, R.string.settings_sync_wifi_hint, state.wifiOnly, onWifiOnly)
            if (state.status is SyncStatus.Failed) {
                OutlineButton(stringResource(R.string.settings_sync_retry), onRetry, Modifier.fillMaxWidth())
            } else {
                OutlineButton(
                    text = stringResource(R.string.settings_sync_now),
                    onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.status != SyncStatus.Syncing,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(status: SyncStatus) {
    when (status) {
        SyncStatus.Disabled -> Unit
        SyncStatus.Syncing -> SyncText(R.string.sync_status_syncing)
        SyncStatus.Pending -> SyncText(R.string.sync_status_pending)
        is SyncStatus.Synced -> {
            val at = status.at
            if (at == null) SyncText(R.string.sync_status_synced) else SyncText(R.string.sync_status_synced_at, lastSyncText(at))
        }
        is SyncStatus.Failed -> SyncText(status.error.messageRes(), error = true)
    }
}

@Composable
private fun SyncText(@StringRes text: Int, vararg args: Any, error: Boolean = false) {
    Text(
        text = stringResource(text, *args),
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "hace 5 minutos", or "hace un momento" for the first minute. */
@Composable
private fun lastSyncText(at: Long): String {
    val now = System.currentTimeMillis()
    if (now - at < DateUtils.MINUTE_IN_MILLIS) return stringResource(R.string.sync_just_now)
    return DateUtils.getRelativeTimeSpanString(at, now, DateUtils.MINUTE_IN_MILLIS).toString()
}

@StringRes
internal fun SyncError.messageRes(): Int = when (this) {
    is SyncError.NotAuthorized -> R.string.sync_error_not_authorized
    is SyncError.QuotaExceeded -> R.string.sync_error_quota
    is SyncError.NoInternet -> R.string.sync_error_no_internet
    is SyncError.NewerFormat -> R.string.sync_error_newer_format
    is SyncError.Conflict, is SyncError.Unknown -> R.string.sync_error_unknown
}
