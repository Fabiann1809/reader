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
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape

/** "Proveedor de IA" (T20.5): which service answers, with which model, and where a compatible one lives. */
@Composable
fun ProviderSection(
    config: AiConfig,
    onProvider: (AiProviderKind) -> Unit,
    onModel: (String) -> Unit,
    onCompatibleUrl: (String) -> Unit,
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
        // Each provider starts its fields over from what is saved for it.
        key(config.provider) {
            if (config.provider == AiProviderKind.COMPATIBLE) CompatibleUrlField(config.compatibleUrl, onCompatibleUrl)
            ModelPicker(config, onModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPicker(config: AiConfig, onModel: (String) -> Unit) {
    // Typed here and saved on every change, so the field never waits for the store.
    var model by rememberSaveable { mutableStateOf(config.model) }
    val change = { text: String ->
        model = text
        onModel(text)
    }
    val suggestions = config.provider.suggestedModels
    var expanded by rememberSaveable { mutableStateOf(false) }
    // A list to pick from; the field stays editable for any other model name.
    ExposedDropdownMenuBox(expanded = expanded && suggestions.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = model,
            onValueChange = change,
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
                        change(suggested)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CompatibleUrlField(saved: String, onChange: (String) -> Unit) {
    var url by rememberSaveable { mutableStateOf(saved) }
    // The key travels in every request, so an address without https is not accepted.
    val notSecure = url.isNotBlank() && !url.trim().startsWith("https://")
    OutlinedTextField(
        value = url,
        onValueChange = {
            url = it
            onChange(it)
        },
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
