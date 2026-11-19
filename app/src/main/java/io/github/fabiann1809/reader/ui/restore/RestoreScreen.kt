package io.github.fabiann1809.reader.ui.restore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.auth.ReaderLogo
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.account.messageRes

/** The loading screen between signing in and the library when the library is coming back from Drive (T18.9b). */
@Composable
fun RestoreScreen(state: RestoreState, onContinue: () -> Unit, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
    ) {
        ReaderLogo(iconSize = 60.dp)
        Spacer(Modifier.height(40.dp))
        if (state is RestoreState.Failed) Failed(state, onContinue, onRetry) else Restoring(onContinue)
    }
}

@Composable
private fun Restoring(onContinue: () -> Unit) {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.restore_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.restore_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.restore_wifi_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    TextButton(onClick = onContinue) { Text(stringResource(R.string.restore_continue)) }
}

@Composable
private fun Failed(state: RestoreState.Failed, onContinue: () -> Unit, onRetry: () -> Unit) {
    Text(
        text = stringResource(R.string.restore_failed_title),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(state.error.messageRes()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    PrimaryButton(text = stringResource(R.string.restore_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = onContinue) { Text(stringResource(R.string.restore_failed_continue)) }
}
