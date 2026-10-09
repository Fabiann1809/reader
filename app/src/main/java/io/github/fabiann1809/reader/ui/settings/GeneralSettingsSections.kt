package io.github.fabiann1809.reader.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.AppTheme
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.library.labelRes

/** "Apariencia": the app's theme, the library's default view and books per row. */
@Composable
fun AppearanceSection(
    state: GeneralSettingsUiState,
    onTheme: (AppTheme) -> Unit,
    onView: (LibraryView) -> Unit,
    onBooksPerRow: (Int) -> Unit,
) {
    SectionTitle(R.string.settings_appearance)
    Choice(R.string.settings_theme) {
        AppTheme.entries.forEach { theme ->
            Chip(stringResource(theme.label()), selected = theme == state.settings.theme) { onTheme(theme) }
        }
    }
    Choice(R.string.settings_default_view) {
        LibraryView.entries.forEach { view ->
            Chip(stringResource(view.labelRes()), selected = view == state.layout.view) { onView(view) }
        }
    }
    // The list shows one book per row: the count only matters for shelves and the grid.
    if (state.layout.view != LibraryView.LIST) {
        Choice(R.string.arrange_books_per_row) {
            LibraryLayout.BooksPerRowRange.forEach { count ->
                Chip(count.toString(), selected = count == state.layout.booksPerRow) { onBooksPerRow(count) }
            }
        }
    }
}

/** "Lectura": the reader's "Aa" settings, which are every book's defaults. */
@Composable
fun ReadingDefaultsSection(onOpen: () -> Unit) {
    SectionTitle(R.string.settings_reading)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(vertical = 8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_text_aa), contentDescription = null, modifier = Modifier.size(24.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(stringResource(R.string.settings_reading_defaults), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_reading_defaults_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(painterResource(R.drawable.ic_caret_right), contentDescription = null)
    }
}

/** The AI features that run without being asked, which can be turned off. */
@Composable
fun AiFeaturesSection(state: GeneralSettingsUiState, onAutoTranscribe: (Boolean) -> Unit, onChapterEnd: (Boolean) -> Unit) {
    SectionTitle(R.string.settings_ai_section)
    Toggle(R.string.settings_auto_transcribe, R.string.settings_auto_transcribe_hint, state.settings.autoTranscribe, onAutoTranscribe)
    Toggle(R.string.settings_chapter_end, R.string.settings_chapter_end_hint, state.settings.chapterEndSuggestion, onChapterEnd)
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Choice(@StringRes label: Int, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    ReaderFilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun Toggle(@StringRes title: Int, @StringRes hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun SettingsDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@StringRes
private fun AppTheme.label(): Int = when (this) {
    AppTheme.SYSTEM -> R.string.settings_theme_system
    AppTheme.LIGHT -> R.string.settings_theme_light
    AppTheme.DARK -> R.string.settings_theme_dark
}
