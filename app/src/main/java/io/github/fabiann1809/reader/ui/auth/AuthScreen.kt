package io.github.fabiann1809.reader.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton

/** Sign-in and account creation (T22.3). Shown whenever nobody is signed in. */
@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::submit,
        onGoogle = { viewModel.signInWithGoogle { requestGoogleIdToken(context, webClientId) } },
        onForgotPassword = viewModel::resetPassword,
        onSwitchMode = viewModel::switchMode,
    )
}

@Composable
fun AuthContent(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogle: () -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signingUp = state.mode == AuthMode.SIGN_UP
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (signingUp) R.string.auth_title_sign_up else R.string.auth_title_sign_in),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EmailField(state.email, onEmailChange, state.emailError)
            PasswordField(
                value = state.password,
                onChange = onPasswordChange,
                label = R.string.auth_password,
                error = state.passwordError,
                imeAction = if (signingUp) ImeAction.Next else ImeAction.Done,
                onDone = onSubmit,
            )
            if (signingUp) {
                PasswordField(
                    value = state.confirmPassword,
                    onChange = onConfirmChange,
                    label = R.string.auth_confirm_password,
                    error = state.confirmError,
                    imeAction = ImeAction.Done,
                    onDone = onSubmit,
                )
                PasswordChecklist(state.passwordChecks)
            }
            Message(state)
            PrimaryButton(
                text = stringResource(if (signingUp) R.string.auth_sign_up else R.string.auth_sign_in),
                onClick = onSubmit,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!signingUp) {
                TextButton(onClick = onForgotPassword, enabled = !state.loading) {
                    Text(stringResource(R.string.auth_forgot_password))
                }
            }
            Text(
                text = stringResource(R.string.auth_or),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
            )
            OutlineButton(
                text = stringResource(R.string.auth_google),
                onClick = onGoogle,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onSwitchMode, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (signingUp) R.string.auth_switch_to_sign_in else R.string.auth_switch_to_sign_up))
            }
        }
    }
}

/** The error, or the note that the reset email was sent. */
@Composable
private fun Message(state: AuthUiState) {
    val text = when {
        state.generalError != null -> stringResource(authErrorMessageRes(state.generalError))
        state.resetSent -> stringResource(R.string.auth_reset_sent)
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.generalError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}
