package io.github.fabiann1809.reader.ui.more

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar

/** Third-party assets bundled with the app; their license texts live in assets/licenses. */
private enum class ThirdPartyLicense(@StringRes val title: Int, @StringRes val license: Int, val file: String) {
    MANROPE(R.string.about_font_manrope, R.string.about_license_ofl, "OFL-Manrope.txt"),
    DM_SERIF_DISPLAY(R.string.about_font_dm_serif_display, R.string.about_license_ofl, "OFL-DMSerifDisplay.txt"),

    // Reading fonts of the "Aa" settings (OpenDyslexic comes with Readium, with its license).
    LITERATA(R.string.about_font_literata, R.string.about_license_ofl, "OFL-Literata.txt"),
    MERRIWEATHER(R.string.about_font_merriweather, R.string.about_license_ofl, "OFL-Merriweather.txt"),
    SOURCE_SERIF(R.string.about_font_source_serif, R.string.about_license_ofl, "OFL-SourceSerif4.txt"),
    LORA(R.string.about_font_lora, R.string.about_license_ofl, "OFL-Lora.txt"),
    ATKINSON(R.string.about_font_atkinson, R.string.about_license_ofl, "OFL-AtkinsonHyperlegible.txt"),
    INTER(R.string.about_font_inter, R.string.about_license_ofl, "OFL-Inter.txt"),
    PHOSPHOR(R.string.about_icons_phosphor, R.string.about_license_mit, "MIT-PhosphorIcons.txt"),
}

@Composable
fun AboutScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var openLicense by rememberSaveable { mutableStateOf<ThirdPartyLicense?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.about_title), onNavigateUp = onNavigateUp) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.about_licenses),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            )
            ThirdPartyLicense.entries.forEach { item ->
                ListItem(
                    headlineContent = { Text(stringResource(item.title)) },
                    supportingContent = { Text(stringResource(item.license)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { openLicense = item },
                )
            }
        }
    }

    openLicense?.let { item ->
        val text = remember(item) { context.assets.open("licenses/${item.file}").bufferedReader().use { it.readText() } }
        AlertDialog(
            onDismissRequest = { openLicense = null },
            title = { Text(stringResource(item.title)) },
            text = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { openLicense = null }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}
