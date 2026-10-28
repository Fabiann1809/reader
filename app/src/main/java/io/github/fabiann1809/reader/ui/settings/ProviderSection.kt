package io.github.fabiann1809.reader.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiConfig
import io.github.fabiann1809.reader.ai.AiProviderKind
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.ui.components.TonalButton
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

/**
 * "Proveedor de IA" (T20.5): which service answers, with which model, and where a compatible one lives.
 * Picking a provider applies at once; the model and the address are a draft until "Guardar cambios".
 */
@Composable
fun ProviderSection(
    config: AiConfig,
    onProvider: (AiProviderKind) -> Unit,
    onSave: (model: String, url: String) -> Unit,
) {
    SettingsSection(R.string.settings_provider_section) {
        Text(
            stringResource(R.string.settings_provider_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProviderKind.entries.forEach { kind ->
                ReaderFilterChip(
                    selected = kind == config.provider,
                    onClick = { onProvider(kind) },
                    label = { Text(stringResource(kind.label())) },
                )
            }
        }
        // Each provider starts its draft over from what is saved for it.
        key(config.provider) {
            var model by rememberSaveable { mutableStateOf(config.model) }
            var url by rememberSaveable { mutableStateOf(config.compatibleUrl) }
            val isCompatible = config.provider == AiProviderKind.COMPATIBLE
            // The key travels in every request, so an address without https is not accepted.
            val notSecure = isCompatible && url.isNotBlank() && !url.trim().startsWith("https://")
            // An empty model means the provider's default one.
            val changed = model.trim().ifEmpty { config.provider.defaultModel } != config.model ||
                (isCompatible && url.trim() != config.compatibleUrl)
            if (isCompatible) CompatibleUrlField(url, notSecure) { url = it }
            ModelPicker(config.provider, model) { model = it }
            SaveButton(changed = changed, enabled = changed && !notSecure) { onSave(model, url) }
        }
    }
}

@Composable
private fun SaveButton(changed: Boolean, enabled: Boolean, onClick: () -> Unit) {
    TonalButton(
        text = stringResource(if (changed) R.string.settings_provider_save else R.string.settings_provider_saved_state),
        onClick = onClick,
        enabled = enabled,
        icon = if (changed) null else R.drawable.ic_check,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(provider: AiProviderKind, model: String, onChange: (String) -> Unit) {
    val suggestions = provider.suggestedModels
    var expanded by rememberSaveable { mutableStateOf(false) }
    // A list to pick from; the field stays editable for any other model name.
    ExposedDropdownMenuBox(expanded = expanded && suggestions.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = model,
            onValueChange = onChange,
            shape = readerTextFieldShape,
            colors = readerTextFieldColors(),
            label = { Text(stringResource(R.string.settings_model_field)) },
            supportingText = {
                Text(stringResource(if (suggestions.isEmpty()) R.string.settings_model_hint_compatible else R.string.settings_model_hint))
            },
            trailingIcon = { if (suggestions.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded && suggestions.isNotEmpty(), onDismissRequest = { expanded = false }) {
            suggestions.forEach { suggested ->
                DropdownMenuItem(
                    text = { Text(suggested) },
                    onClick = {
                        onChange(suggested)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CompatibleUrlField(url: String, notSecure: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = url,
        onValueChange = onChange,
        shape = readerTextFieldShape,
        colors = readerTextFieldColors(),
        label = { Text(stringResource(R.string.settings_url_field)) },
        supportingText = { Text(stringResource(if (notSecure) R.string.settings_url_not_https else R.string.settings_url_hint)) },
        isError = notSecure,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

@StringRes
fun AiProviderKind.label(): Int = when (this) {
    AiProviderKind.GEMINI -> R.string.settings_provider_gemini
    AiProviderKind.OPENAI -> R.string.settings_provider_openai
    AiProviderKind.ANTHROPIC -> R.string.settings_provider_anthropic
    AiProviderKind.COMPATIBLE -> R.string.settings_provider_compatible
}
