package io.github.fabiann1809.reader.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private val GoogleBlue = Color(0xFF4285F4)
private const val FOOTER_HEIGHT = 96

/**
 * Sign in, create an account or recover the password (T22.3, restyled in T22.8). Shown whenever
 * nobody is signed in. [onBack] goes back to the welcome.
 */
@Composable
fun AuthScreen(
    startOnSignUp: Boolean,
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // Only when it differs, so turning the phone does not wipe what was typed.
    LaunchedEffect(startOnSignUp) {
        val wanted = if (startOnSignUp) AuthMode.SIGN_UP else AuthMode.SIGN_IN
        if (viewModel.uiState.value.mode != wanted && viewModel.uiState.value.mode != AuthMode.FORGOT) viewModel.open(wanted)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    val goBack = { if (state.mode == AuthMode.FORGOT) viewModel.showSignIn() else onBack() }
    BackHandler(onBack = goBack)
    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::submit,
        onGoogle = { viewModel.signInWithGoogle { requestGoogleIdToken(context, webClientId) } },
        onForgotPassword = viewModel::showForgotPassword,
        onSwitchMode = viewModel::switchMode,
        onBackToSignIn = viewModel::showSignIn,
        onResend = viewModel::resendReset,
        onBack = goBack,
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
    onBackToSignIn: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(110.dp, (-140).dp)
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(ReaderTheme.colors.authWave),
            )
            Footer(Modifier.align(Alignment.BottomCenter))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column(Modifier.padding(horizontal = 24.dp).padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BackButton(onBack, Modifier.align(Alignment.Start))
                    ReaderLogo(iconSize = 60.dp, modifier = Modifier.padding(top = 4.dp))
                    if (state.mode == AuthMode.FORGOT && state.resetSent) {
                        SentStep(state, onBackToSignIn, onResend)
                    } else {
                        Form(state, onEmailChange, onPasswordChange, onConfirmChange, onSubmit, onGoogle, onForgotPassword, onSwitchMode, onBackToSignIn)
                    }
                }
                Spacer(Modifier.height(FOOTER_HEIGHT.dp))
            }
        }
    }
}

@Composable
private fun Form(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogle: () -> Unit,
    onForgotPassword: () -> Unit,
    onSwitchMode: () -> Unit,
    onBackToSignIn: () -> Unit,
) {
    val mode = state.mode
    val signingUp = mode == AuthMode.SIGN_UP
    val forgot = mode == AuthMode.FORGOT
    Titles(mode)
    Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EmailField(
            value = state.email,
            onChange = onEmailChange,
            error = state.emailError,
            imeAction = if (forgot) ImeAction.Done else ImeAction.Next,
            onDone = onSubmit,
        )
        if (!forgot) {
            PasswordField(
                value = state.password,
                onChange = onPasswordChange,
                placeholder = R.string.auth_password,
                error = state.passwordError,
                imeAction = if (signingUp) ImeAction.Next else ImeAction.Done,
                onDone = onSubmit,
            )
        }
        if (signingUp) {
            PasswordField(
                value = state.confirmPassword,
                onChange = onConfirmChange,
                placeholder = R.string.auth_confirm_password,
                error = state.confirmError,
                imeAction = ImeAction.Done,
                onDone = onSubmit,
            )
            PasswordChecklist(state.passwordChecks)
        }
        if (mode == AuthMode.SIGN_IN) {
            ForgotLink(onForgotPassword, enabled = !state.loading, modifier = Modifier.align(Alignment.End))
        }
        ErrorBanner(state)
        PrimaryButton(
            text = stringResource(
                when (mode) {
                    AuthMode.SIGN_UP -> R.string.auth_sign_up
                    AuthMode.FORGOT -> R.string.auth_send_link
                    AuthMode.SIGN_IN -> R.string.auth_sign_in
                },
            ),
            onClick = onSubmit,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        )
        if (forgot) {
            TextButton(onClick = onBackToSignIn, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.auth_back_to_sign_in), fontWeight = FontWeight.ExtraBold)
            }
        } else {
            OrDivider()
            GoogleButton(onGoogle, enabled = !state.loading)
            SwitchModeRow(signingUp, onSwitchMode, enabled = !state.loading)
        }
    }
}

@Composable
private fun Titles(mode: AuthMode) {
    val (title, subtitle) = when (mode) {
        AuthMode.SIGN_IN -> R.string.auth_title_sign_in to R.string.auth_subtitle_sign_in
        AuthMode.SIGN_UP -> R.string.auth_title_sign_up to R.string.auth_subtitle_sign_up
        AuthMode.FORGOT -> R.string.auth_title_forgot to R.string.auth_subtitle_forgot
    }
    Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}

/** The 48 dp round button at the top left. */
@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_caret_left),
                contentDescription = stringResource(R.string.navigate_up),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ForgotLink(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.offset(y = (-6).dp)) {
        Text(stringResource(R.string.auth_forgot_password), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** What the service answered that belongs to no field: wrong password, no network and so on. */
@Composable
private fun ErrorBanner(state: AuthUiState) {
    val error = state.generalError ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(authErrorMessageRes(error)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit, enabled: Boolean) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, ReaderTheme.colors.authFieldBorder),
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 22.dp)) {
            Icon(painterResource(R.drawable.ic_google_logo), contentDescription = null, tint = GoogleBlue, modifier = Modifier.size(22.dp))
            Text(
                text = stringResource(R.string.auth_google),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** "¿Aún no tienes cuenta? Crea una", or the other way round. */
@Composable
private fun SwitchModeRow(signingUp: Boolean, onClick: () -> Unit, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(if (signingUp) R.string.auth_have_account else R.string.auth_no_account),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 6.dp),
        ) {
            Text(
                text = stringResource(if (signingUp) R.string.auth_sign_in_link else R.string.auth_create_one),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** After asking for the reset link: where it went, and the way back or to send it again. */
@Composable
private fun SentStep(state: AuthUiState, onBackToSignIn: () -> Unit, onResend: () -> Unit) {
    val success = ReaderTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = stringResource(R.string.auth_sent_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = 18.dp).size(84.dp).clip(CircleShape).background(success.successContainer),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_envelope_simple_open_fill),
                contentDescription = null,
                tint = success.success,
                modifier = Modifier.size(38.dp),
            )
        }
        Text(
            text = stringResource(R.string.auth_sent_text, state.email.trim()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        ErrorBanner(state)
        PrimaryButton(
            text = stringResource(R.string.auth_back_to_sign_in),
            onClick = onBackToSignIn,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 58.dp),
        )
        TextButton(onClick = onResend, enabled = !state.loading, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = stringResource(if (state.resent) R.string.auth_resent else R.string.auth_resend),
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/** The wave with a small open book between two lines at the bottom. */
@Composable
private fun Footer(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(FOOTER_HEIGHT.dp)) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = 20.dp)
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(ReaderTheme.colors.authWave),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 44.dp),
        ) {
            val line = ReaderTheme.colors.authFieldBorder
            Box(Modifier.size(64.dp, 2.dp).clip(CircleShape).background(line))
            Icon(painterResource(R.drawable.ic_book_open_bold), contentDescription = null, tint = line, modifier = Modifier.size(34.dp))
            Box(Modifier.size(64.dp, 2.dp).clip(CircleShape).background(line))
        }
    }
}
