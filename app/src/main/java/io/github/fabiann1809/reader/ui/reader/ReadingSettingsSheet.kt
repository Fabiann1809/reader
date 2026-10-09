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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.PageEffect
import io.github.fabiann1809.reader.data.prefs.ReadingAlignment
import io.github.fabiann1809.reader.data.prefs.ReadingFont
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.prefs.ReadingTheme
import kotlin.math.roundToInt

/**
 * The reader's "Aa" sheet (design 10.12, T11.7): theme (with the quick day/night switch), font,
 * size, line height, margins and alignment, then the page settings (T11.9). Every change shows at
 * once and is saved. PDFs keep their own look, so instead of the text settings they get a note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    isPdf: Boolean,
    onChange: ((ReadingSettings) -> ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            if (isPdf) {
                Text(stringResource(R.string.reading_settings_pdf), style = MaterialTheme.typography.bodyMedium)
            } else {
                TextSettings(settings, onChange)
            }
            Section(R.string.reading_settings_page) {
                // The PDF view turns its pages its own way.
                if (!isPdf) PageEffectChoice(settings.pageEffect) { effect -> onChange { it.copy(pageEffect = effect) } }
                PageSettings(settings, onChange)
            }
        }
    }
}

@Composable
private fun TextSettings(settings: ReadingSettings, onChange: ((ReadingSettings) -> ReadingSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(R.string.reading_settings_theme) {
            ThemeRow(settings) { theme -> onChange { it.copy(theme = theme) } }
            if (settings.theme == ReadingTheme.CUSTOM) CustomThemePicker(settings, onChange)
        }
        Section(R.string.reading_settings_font) { FontPicker(settings.font) { font -> onChange { it.copy(font = font) } } }
        Section(R.string.reading_settings_size) {
            SizeStepper(settings.fontSize) { size -> onChange { it.copy(fontSize = size) } }
        }
        Section(R.string.reading_settings_line_height) {
            SettingSlider(settings.lineHeight, ReadingSettings.LineHeightRange, R.string.reading_settings_line_height) { value ->
                onChange { it.copy(lineHeight = value) }
            }
        }
        Section(R.string.reading_settings_margins) {
            SettingSlider(settings.margins, ReadingSettings.MarginsRange, R.string.reading_settings_margins) { value ->
                onChange { it.copy(margins = value) }
            }
        }
        Section(R.string.reading_settings_alignment) {
            AlignmentChoice(settings.alignment) { alignment -> onChange { it.copy(alignment = alignment) } }
        }
    }
}

/** The indicators under the page and keeping the screen on (T11.9); they apply to EPUB and PDF. */
@Composable
private fun PageSettings(settings: ReadingSettings, onChange: ((ReadingSettings) -> ReadingSettings) -> Unit) {
    Column {
        SwitchRow(R.string.reading_settings_show_clock, settings.showClock) { on -> onChange { it.copy(showClock = on) } }
        SwitchRow(R.string.reading_settings_show_battery, settings.showBattery) { on -> onChange { it.copy(showBattery = on) } }
        SwitchRow(R.string.reading_settings_show_page, settings.showPage) { on -> onChange { it.copy(showPage = on) } }
        SwitchRow(R.string.reading_settings_keep_screen_on, settings.keepScreenOn) { on -> onChange { it.copy(keepScreenOn = on) } }
    }
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // The whole row toggles; the switch only shows the state. Off, the thumb takes the inactive
        // icon color: Material's default matches this theme's track and would disappear.
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** A swatch per theme: its page color with "Aa" in its text color, and the name below. */
@Composable
private fun ThemeRow(settings: ReadingSettings, onSelect: (ReadingTheme) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        ReadingTheme.entries.forEach { theme ->
            // "Personalizado" shows the colors picked for it.
            val colors = if (theme == ReadingTheme.CUSTOM) settings.copy(theme = theme).pageColors() else theme.colors()
            val isSelected = theme == settings.theme
            val name = stringResource(theme.label)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable(role = Role.RadioButton) { onSelect(theme) }
                    .semantics(mergeDescendants = true) { this.selected = isSelected }
                    .padding(4.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 56.dp, height = 52.dp)
                        .scale(if (isSelected) 1.08f else 1f)
                        .then(
                            if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, TileShape) else Modifier,
                        )
                        .padding(if (isSelected) 3.dp else 0.dp)
                        .background(Color(colors.background), TileShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, TileShape),
                ) {
                    Text(
                        stringResource(R.string.reader_text_settings),
                        color = Color(colors.text),
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FontPicker(selected: ReadingFont, onSelect: (ReadingFont) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        ReadingFont.entries.forEach { font ->
            ReaderFilterChip(
                selected = font == selected,
                onClick = { onSelect(font) },
                label = { Text(stringResource(font.label)) },
            )
        }
    }
}

/** "A−  100 %  A+": the font size, one step of 0.1 at a time. */
@Composable
private fun SizeStepper(value: Double, onChange: (Double) -> Unit) {
    val range = ReadingSettings.FontSizeRange
    val label = stringResource(R.string.reading_settings_size)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.weight(1f))
        StepButton(R.string.reading_settings_size_smaller, "A\u2212", enabled = value > range.start) {
            onChange(roundToStep((value - SIZE_STEP).toFloat()).coerceIn(range))
        }
        Text(
            text = stringResource(R.string.reading_settings_size_value, (value * 100).roundToInt()),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(56.dp)
                .semantics { contentDescription = label },
        )
        StepButton(R.string.reading_settings_size_bigger, "A+", enabled = value < range.endInclusive) {
            onChange(roundToStep((value + SIZE_STEP).toFloat()).coerceIn(range))
        }
    }
}

private const val SIZE_STEP = 0.1

@Composable
private fun StepButton(@StringRes description: Int, text: String, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(description)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clearAndSetSemantics {})
    }
}

/** A slider in steps of 0.1 that saves when the finger lifts, so dragging doesn't write on every move. */
@Composable
private fun SettingSlider(
    value: Double,
    range: ClosedFloatingPointRange<Double>,
    @StringRes description: Int,
    modifier: Modifier = Modifier,
    onChange: (Double) -> Unit,
) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val label = stringResource(description)
    ReaderSlider(
        value = dragged ?: value.toFloat(),
        onValueChange = { dragged = it },
        onValueChangeFinished = {
            dragged?.let { onChange(roundToStep(it)) }
            dragged = null
        },
        valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
        steps = stepsIn(range),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
    )
}

/** Deslizar, Continuo o Ninguno (T11.8). */
@Composable
private fun PageEffectChoice(selected: PageEffect, onSelect: (PageEffect) -> Unit) {
    val options = listOf(
        PageEffect.SLIDE to R.string.page_effect_slide,
        PageEffect.SCROLL to R.string.page_effect_scroll,
        PageEffect.NONE to R.string.page_effect_none,
    )
    ChoicePills(options, selected, onSelect)
}

@Composable
private fun AlignmentChoice(selected: ReadingAlignment, onSelect: (ReadingAlignment) -> Unit) {
    val options = listOf(
        ReadingAlignment.START to R.string.reading_settings_align_start,
        ReadingAlignment.JUSTIFY to R.string.reading_settings_align_justify,
    )
    ChoicePills(options, selected, onSelect)
}

/** The options of a setting as equal pills in a row; the chosen one is filled with the accent. */
@Composable
private fun <T> ChoicePills(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (option, label) ->
            ReaderFilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(stringResource(label), maxLines = 1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val TileShape = RoundedCornerShape(16.dp)

// Tenths: the slider snaps to them and the saved value is exact.
private fun roundToStep(value: Float): Double = (value * 10).roundToInt() / 10.0

private fun stepsIn(range: ClosedFloatingPointRange<Double>): Int = ((range.endInclusive - range.start) * 10).roundToInt() - 1

private val ReadingTheme.label: Int
    get() = when (this) {
        ReadingTheme.DAY -> R.string.reading_theme_day
        ReadingTheme.SEPIA -> R.string.reading_theme_sepia
        ReadingTheme.PAPER_GRAY -> R.string.reading_theme_paper_gray
        ReadingTheme.NIGHT -> R.string.reading_theme_night
        ReadingTheme.AMOLED -> R.string.reading_theme_amoled
        ReadingTheme.CUSTOM -> R.string.reading_theme_custom
    }

private val ReadingFont.label: Int
    get() = when (this) {
        ReadingFont.LITERATA -> R.string.reading_font_literata
        ReadingFont.MERRIWEATHER -> R.string.reading_font_merriweather
        ReadingFont.SOURCE_SERIF -> R.string.reading_font_source_serif
        ReadingFont.LORA -> R.string.reading_font_lora
        ReadingFont.ATKINSON -> R.string.reading_font_atkinson
        ReadingFont.OPEN_DYSLEXIC -> R.string.reading_font_open_dyslexic
        ReadingFont.INTER -> R.string.reading_font_inter
    }
