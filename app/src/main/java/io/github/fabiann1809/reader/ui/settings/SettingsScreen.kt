package io.github.fabiann1809.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.reader.ReadingSettingsSheet
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    general: GeneralSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val generalState by general.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onKeyInputChange = viewModel::onKeyInputChange,
        onSaveKey = viewModel::saveKey,
        onClearKey = viewModel::clearKey,
        onTestKey = viewModel::testKey,
        onMessageShown = viewModel::onMessageShown,
        onOpenPrivacy = onOpenPrivacy,
        onNavigateUp = onNavigateUp,
        // T17.1: appearance, reading defaults and the AI features, around the API key.
        generalSettings = {
            AppearanceSection(generalState, onTheme = general::setTheme, onView = general::setLibraryView, onBooksPerRow = general::setBooksPerRow)
            ReadingDefaultsSection(onOpen = general::showReadingSettings)
            AiFeaturesSection(generalState, onAutoTranscribe = general::setAutoTranscribe, onChapterEnd = general::setChapterEndSuggestion)
        },
    )
    if (generalState.readingSheetVisible) {
        ReadingSettingsSheet(
            settings = generalState.reading,
            isPdf = false,
            onChange = general::updateReadingSettings,
            onDismiss = general::hideReadingSettings,
        )
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onTestKey: () -> Unit,
    onMessageShown: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    generalSettings: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        val text = when (message) {
            SettingsMessage.KEY_SAVED -> R.string.settings_key_saved
            SettingsMessage.KEY_CLEARED -> R.string.settings_key_cleared
        }
        snackbarHostState.showSnackbar(resources.getString(text))
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
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ApiKeySection(uiState, onKeyInputChange, onSaveKey, onClearKey, onTestKey)
            generalSettings()
            PrivacyCard(onClick = onOpenPrivacy)
        }
    }
}

/** The green "Privacidad" card; tapping it opens the full explanation. */
@Composable
private fun PrivacyCard(onClick: () -> Unit) {
    val colors = ReaderTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.successContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield_check),
            contentDescription = null,
            tint = colors.success,
            modifier = Modifier.size(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.onSuccessContainer,
            )
            Text(
                stringResource(R.string.settings_privacy_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_caret_right),
            contentDescription = null,
            tint = colors.onSuccessContainer,
            modifier = Modifier.size(18.dp),
        )
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
            onTestKey = {},
            onMessageShown = {},
            onOpenPrivacy = {},
            onNavigateUp = {},
        )
    }
}
