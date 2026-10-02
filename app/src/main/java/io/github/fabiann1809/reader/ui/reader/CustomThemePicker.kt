package io.github.fabiann1809.reader.ui.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.ReadingSettings

/** "Personalizado" (T11.7): the page color and the text color, each from its palette. */
@Composable
fun CustomThemePicker(settings: ReadingSettings, onChange: ((ReadingSettings) -> ReadingSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PaletteRow(R.string.reading_custom_background, CustomBackgrounds, settings.customBackground) { color ->
            onChange { it.copy(customBackground = color) }
        }
        PaletteRow(R.string.reading_custom_text, CustomTexts, settings.customText) { color ->
            onChange { it.copy(customText = color) }
        }
    }
}

@Composable
private fun PaletteRow(@StringRes title: Int, colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    val name = stringResource(title)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            colors.forEachIndexed { index, color ->
                val isSelected = color == selected
                // Screen readers hear "Fondo, color 3": the palette has no color names.
                val description = stringResource(R.string.reading_custom_color, name, index + 1)
                Box(
                    Modifier
                        .size(36.dp)
                        .background(Color(color), CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable(role = Role.RadioButton) { onSelect(color) }
                        .semantics {
                            contentDescription = description
                            this.selected = isSelected
                        },
                )
            }
        }
    }
}
