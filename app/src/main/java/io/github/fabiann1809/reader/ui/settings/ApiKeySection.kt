package io.github.fabiann1809.reader.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private const val API_KEY_URL = "https://aistudio.google.com/apikey"

/** "Clave de API · Google Gemini": its status, the field, save / clear / test and how to get a key. */
@Composable
fun ApiKeySection(
    uiState: SettingsUiState,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onTestKey: () -> Unit,
) {
    SettingsSection(R.string.settings_api_key_section) {
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
    val colors = ReaderTheme.colors
    val scheme = MaterialTheme.colorScheme
    when (state) {
        KeyTestState.Idle -> Unit
        KeyTestState.Testing -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.ai)
            Text(stringResource(R.string.settings_testing_key), style = MaterialTheme.typography.bodyMedium)
        }
        is KeyTestState.Success -> ResultBanner(
            icon = R.drawable.ic_check_circle,
            accent = colors.success,
            container = colors.successContainer,
            title = stringResource(R.string.settings_test_success),
            message = state.sampleResponse,
        )
        is KeyTestState.Failure -> ResultBanner(
            icon = R.drawable.ic_warning_circle,
            accent = scheme.error,
            container = scheme.errorContainer,
            title = stringResource(aiErrorMessageRes(state.error)),
        )
    }
}

@Composable
private fun ResultBanner(@DrawableRes icon: Int, accent: Color, container: Color, title: String, message: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            message?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ApiKeyGuide() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(stringResource(R.string.settings_guide_title), style = MaterialTheme.typography.titleSmall)
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
