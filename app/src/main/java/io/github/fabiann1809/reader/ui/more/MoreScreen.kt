package io.github.fabiann1809.reader.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar

/** "Más" tab: secondary destinations. T8.4 adds notes, backup, privacy and about. */
@Composable
fun MoreScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.tab_more)) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_title)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_gear_six),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Icon(painterResource(R.drawable.ic_caret_right), contentDescription = null)
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable(onClick = onOpenSettings),
            )
        }
    }
}
