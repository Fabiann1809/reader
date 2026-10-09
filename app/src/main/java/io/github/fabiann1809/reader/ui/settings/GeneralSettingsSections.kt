package io.github.fabiann1809.reader.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.AppTheme
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.components.SegmentedControl
import io.github.fabiann1809.reader.ui.library.labelRes

/** A small grey label over a rounded card, like each block of the prototype's settings. */
@Composable
fun SettingsSection(@StringRes title: Int, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                .padding(16.dp),
            content = content,
        )
    }
}

/** "Apariencia": the app's theme, the library's default view and books per row. */
@Composable
fun AppearanceSection(
    state: GeneralSettingsUiState,
    onTheme: (AppTheme) -> Unit,
    onView: (LibraryView) -> Unit,
    onBooksPerRow: (Int) -> Unit,
) {
    SettingsSection(R.string.settings_appearance) {
        Choice(R.string.settings_theme) {
            SegmentedControl(AppTheme.entries, state.settings.theme, { stringResource(it.label()) }, onTheme)
        }
        Choice(R.string.settings_default_view) {
            SegmentedControl(LibraryView.entries, state.layout.view, { stringResource(it.labelRes()) }, onView)
        }
        // The list shows one book per row: the count only matters for shelves and the grid.
        if (state.layout.view != LibraryView.LIST) {
            Choice(R.string.arrange_books_per_row) {
                SegmentedControl(LibraryLayout.BooksPerRowRange.toList(), state.layout.booksPerRow, { it.toString() }, onBooksPerRow)
            }
        }
    }
}

/** "Lectura": the reader's "Aa" settings, which are every book's defaults. */
@Composable
fun ReadingDefaultsSection(onOpen: () -> Unit) {
    SettingsSection(R.string.settings_reading) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen),
        ) {
            IconBadge(R.drawable.ic_text_aa)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    stringResource(R.string.settings_reading_defaults),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    stringResource(R.string.settings_reading_defaults_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painterResource(R.drawable.ic_caret_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The AI features that run without being asked, which can be turned off. */
@Composable
fun AiFeaturesSection(state: GeneralSettingsUiState, onAutoTranscribe: (Boolean) -> Unit, onChapterEnd: (Boolean) -> Unit) {
    SettingsSection(R.string.settings_ai_section) {
        Toggle(R.string.settings_auto_transcribe, R.string.settings_auto_transcribe_hint, state.settings.autoTranscribe, onAutoTranscribe)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Toggle(R.string.settings_chapter_end, R.string.settings_chapter_end_hint, state.settings.chapterEndSuggestion, onChapterEnd)
    }
}

/** The 40 dp accent-soft circle that leads a row. */
@Composable
fun IconBadge(icon: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Choice(@StringRes label: Int, control: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        control()
    }
}

@Composable
private fun Toggle(@StringRes title: Int, @StringRes hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) },
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@StringRes
private fun AppTheme.label(): Int = when (this) {
    AppTheme.SYSTEM -> R.string.settings_theme_system
    AppTheme.LIGHT -> R.string.settings_theme_light
    AppTheme.DARK -> R.string.settings_theme_dark
}
