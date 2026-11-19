package io.github.fabiann1809.reader.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.settings.SettingsSection

/** "Cuenta": the email in use and "Cerrar sesión". */
@Composable
fun AccountSection(email: String?, state: AccountUiState, onSignOut: () -> Unit) {
    SettingsSection(R.string.settings_account) {
        if (email != null) {
            Text(
                text = stringResource(R.string.settings_account_signed_in, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SignOutControl(state, onSignOut)
    }
}

/**
 * "Cerrar sesión" with what goes around it: the progress and the failure of saving to Drive first, and,
 * with the sync on, a confirmation, because it empties the phone (T18.8). Used in Más and in Cuenta.
 */
@Composable
fun SignOutControl(state: AccountUiState, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        if (state.signingOut) {
            Text(
                text = stringResource(R.string.settings_account_sign_out_saving),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.saveFailed) {
            Text(
                text = stringResource(R.string.settings_account_sign_out_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlineButton(
            text = stringResource(R.string.settings_account_sign_out),
            onClick = { if (state.syncOn) confirming = true else onSignOut() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.signingOut,
        )
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.settings_account_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.settings_account_sign_out_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onSignOut()
                    },
                ) { Text(stringResource(R.string.settings_account_sign_out_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
