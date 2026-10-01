package io.github.fabiann1809.reader.ui.more

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar

/** Secondary destinations of the "Más" tab (design 02 §1), in the design's order. */
enum class MoreEntry(@StringRes val title: Int, @DrawableRes val icon: Int) {
    ALL_NOTES(R.string.all_notes_title, R.drawable.ic_note_pencil),
    BACKUP(R.string.backup_title, R.drawable.ic_archive),
    SETTINGS(R.string.settings_title, R.drawable.ic_gear_six),
    PRIVACY(R.string.settings_privacy, R.drawable.ic_shield_check),
    ABOUT(R.string.about_title, R.drawable.ic_info),
}

@Composable
fun MoreScreen(onOpen: (MoreEntry) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.tab_more)) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            MoreEntry.entries.forEach { entry ->
                // A divider separates content (notes, backup) from app settings and information.
                if (entry == MoreEntry.SETTINGS) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                ListItem(
                    headlineContent = { Text(stringResource(entry.title)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(entry.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_caret_right),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { onOpen(entry) },
                )
            }
        }
    }
}
