package io.github.fabiann1809.reader.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private const val API_KEY_URL = "https://aistudio.google.com/apikey"

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onKeyInputChange = viewModel::onKeyInputChange,
        onSaveKey = viewModel::saveKey,
        onClearKey = viewModel::clearKey,
        onMessageShown = viewModel::onMessageShown,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onMessageShown: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        val text = when (message) {
            SettingsMessage.KEY_SAVED -> R.string.settings_key_saved
            SettingsMessage.KEY_CLEARED -> R.string.settings_key_cleared
        }
        snackbarHostState.showSnackbar(context.getString(text))
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
            Text(stringResource(R.string.settings_ai_section), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(
                    if (uiState.hasApiKey) R.string.settings_key_status_set else R.string.settings_key_status_missing,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = if (uiState.hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            ApiKeyField(value = uiState.keyInput, onValueChange = onKeyInputChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveKey, enabled = uiState.canSaveKey, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_save_key))
                }
                OutlinedButton(onClick = onClearKey, enabled = uiState.hasApiKey, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_clear_key))
                }
            }
            ApiKeyGuide()
        }
    }
}

@Composable
private fun ApiKeyField(value: String, onValueChange: (String) -> Unit) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.settings_key_field)) },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    painter = painterResource(if (isVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = stringResource(
                        if (isVisible) R.string.settings_hide_key else R.string.settings_show_key,
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ApiKeyGuide() {
    Card(modifier = Modifier.fillMaxWidth()) {
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
            onMessageShown = {},
            onNavigateUp = {},
        )
    }
}
