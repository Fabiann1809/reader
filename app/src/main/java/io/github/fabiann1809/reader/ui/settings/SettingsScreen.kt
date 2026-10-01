package io.github.fabiann1809.reader.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private const val API_KEY_URL = "https://aistudio.google.com/apikey"

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onKeyInputChange = viewModel::onKeyInputChange,
        onSaveKey = viewModel::saveKey,
        onClearKey = viewModel::clearKey,
        onTestKey = viewModel::testKey,
        onMessageShown = viewModel::onMessageShown,
        onOpenPrivacy = onOpenPrivacy,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onTestKey: () -> Unit,
    onMessageShown: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        val text = when (message) {
            SettingsMessage.KEY_SAVED -> R.string.settings_key_saved
            SettingsMessage.KEY_CLEARED -> R.string.settings_key_cleared
        }
        snackbarHostState.showSnackbar(resources.getString(text))
        onMessageShown()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.settings_title), onNavigateUp = onNavigateUp)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.settings_ai_section), style = MaterialTheme.typography.titleMedium)
            KeyStatus(hasApiKey = uiState.hasApiKey)
            ApiKeyField(value = uiState.keyInput, onValueChange = onKeyInputChange)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.settings_save_key),
                    onClick = onSaveKey,
                    enabled = uiState.canSaveKey,
                    modifier = Modifier.weight(1f),
                )
                OutlineButton(
                    text = stringResource(R.string.settings_clear_key),
                    onClick = onClearKey,
                    enabled = uiState.hasApiKey,
                    modifier = Modifier.weight(1f),
                )
            }
            // Testing the key calls the AI, so it uses the AI style.
            AiButton(
                text = stringResource(R.string.settings_test_key),
                onClick = onTestKey,
                enabled = uiState.canTestKey,
                modifier = Modifier.fillMaxWidth(),
            )
            KeyTestResult(uiState.keyTest)
            ApiKeyGuide()
            PrivacyLink(onClick = onOpenPrivacy)
        }
    }
}

@Composable
private fun ApiKeyField(value: String, onValueChange: (String) -> Unit) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        shape = readerTextFieldShape,
        colors = readerTextFieldColors(),
        label = { Text(stringResource(R.string.settings_key_field)) },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    painter = painterResource(if (isVisible) R.drawable.ic_eye_slash else R.drawable.ic_eye),
                    contentDescription = stringResource(
                        if (isVisible) R.string.settings_hide_key else R.string.settings_show_key,
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Whether a key is stored, with an icon so the state never depends on color alone. */
@Composable
private fun KeyStatus(hasApiKey: Boolean) {
    val color = if (hasApiKey) ReaderTheme.colors.success else ReaderTheme.colors.warning
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(if (hasApiKey) R.drawable.ic_check_circle else R.drawable.ic_warning_circle),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(if (hasApiKey) R.string.settings_key_status_set else R.string.settings_key_status_missing),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun KeyTestResult(state: KeyTestState) {
    when (state) {
        KeyTestState.Idle -> Unit
        KeyTestState.Testing -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ReaderTheme.colors.ai)
            Text(stringResource(R.string.settings_testing_key), style = MaterialTheme.typography.bodyMedium)
        }
        is KeyTestState.Success -> ResultBanner(
            icon = R.drawable.ic_check_circle,
            color = ReaderTheme.colors.success,
            title = stringResource(R.string.settings_test_success),
            message = state.sampleResponse,
        )
        is KeyTestState.Failure -> ResultBanner(
            icon = R.drawable.ic_warning_circle,
            color = MaterialTheme.colorScheme.error,
            title = stringResource(aiErrorMessageRes(state.error)),
        )
    }
}

@Composable
private fun ResultBanner(@DrawableRes icon: Int, color: Color, title: String, message: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            message?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PrivacyLink(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_caret_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ApiKeyGuide() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_guide_title), style = MaterialTheme.typography.titleMedium)
            val linkColor = MaterialTheme.colorScheme.primary
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.settings_guide_step1))
                    append(" ")
                    withLink(
                        LinkAnnotation.Url(
                            url = API_KEY_URL,
                            styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                        ),
                    ) {
                        append(API_KEY_URL)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.settings_guide_step2), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.settings_guide_step3), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.settings_guide_step4), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.settings_guide_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    ReaderTheme {
        SettingsContent(
            uiState = SettingsUiState(hasApiKey = false),
            onKeyInputChange = {},
            onSaveKey = {},
            onClearKey = {},
            onTestKey = {},
            onMessageShown = {},
            onOpenPrivacy = {},
            onNavigateUp = {},
        )
    }
}
