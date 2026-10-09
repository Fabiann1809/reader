package io.github.fabiann1809.reader.ui.more

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider

/** Secondary destinations of the "Más" tab, in the design's order. [hint] is null when it depends on data. */
enum class MoreEntry(@StringRes val title: Int, @DrawableRes val icon: Int, @StringRes val hint: Int?) {
    ALL_NOTES(R.string.all_notes_title, R.drawable.ic_note_pencil, null),
    BACKUP(R.string.backup_title, R.drawable.ic_archive, R.string.more_backup_hint),
    SETTINGS(R.string.settings_title, R.drawable.ic_gear_six, R.string.more_settings_hint),
    PRIVACY(R.string.settings_privacy, R.drawable.ic_shield_check, R.string.more_privacy_hint),
    ABOUT(R.string.about_title, R.drawable.ic_info, R.string.more_about_hint),
}

@Composable
fun MoreScreen(
    onOpen: (MoreEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoreContent(uiState, onOpen, modifier)
}

@Composable
fun MoreContent(uiState: MoreUiState, onOpen: (MoreEntry) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_more),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        KeyStatusCard(hasApiKey = uiState.hasApiKey)
        MoreEntries(uiState, onOpen)
        Text(
            text = stringResource(R.string.more_footer, version),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
        )
    }
}
