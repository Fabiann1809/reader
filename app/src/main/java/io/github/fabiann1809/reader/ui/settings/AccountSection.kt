package io.github.fabiann1809.reader.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.OutlineButton

/** "Cuenta": the email in use and "Cerrar sesión". */
@Composable
fun AccountSection(email: String?, onSignOut: () -> Unit) {
    SettingsSection(R.string.settings_account) {
        if (email != null) {
            Text(
                text = stringResource(R.string.settings_account_signed_in, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlineButton(
            text = stringResource(R.string.settings_account_sign_out),
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
