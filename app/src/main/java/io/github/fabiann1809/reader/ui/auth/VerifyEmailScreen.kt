package io.github.fabiann1809.reader.ui.auth

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton

/** Shown after creating an account with an email address, until the link sent to it is opened (T22.10). */
@Composable
fun VerifyEmailScreen(viewModel: VerifyEmailViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Coming back from the mail app: look without making the person press anything.
    LifecycleResumeEffect(Unit) {
        viewModel.check(byPerson = false)
        onPauseOrDispose {}
    }
    VerifyEmailContent(
        state = state,
        onCheck = { viewModel.check(byPerson = true) },
        onResend = viewModel::resend,
        onUseAnotherEmail = viewModel::useAnotherEmail,
    )
}

@Composable
fun VerifyEmailContent(
    state: VerifyEmailUiState,
    onCheck: () -> Unit,
    onResend: () -> Unit,
    onUseAnotherEmail: () -> Unit,
) {
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
            text = stringResource(R.string.verify_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.verify_body, state.email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.verify_spam_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Message(state)
        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = stringResource(R.string.verify_check),
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.working,
            loading = state.working,
        )
        TextButton(onClick = onResend, enabled = !state.working) { Text(stringResource(R.string.verify_resend)) }
        TextButton(onClick = onUseAnotherEmail, enabled = !state.working) { Text(stringResource(R.string.verify_other_email)) }
    }
}

/** The one line under the text: what just happened, if anything. */
@Composable
private fun Message(state: VerifyEmailUiState) {
    val error = state.error
    val (text, isError) = when {
        error != null -> stringResource(authErrorMessageRes(error)) to true
        state.stillPending -> stringResource(R.string.verify_still_pending) to true
        state.resent -> stringResource(R.string.verify_resent) to false
        else -> return
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
}
