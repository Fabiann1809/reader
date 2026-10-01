package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage

/** "Progreso" tab. Reading sessions and goals arrive in phase 16; until then it shows the empty state. */
@Composable
fun ProgressScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.tab_progress)) },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            StatusMessage(
                icon = R.drawable.ic_chart_donut,
                title = stringResource(R.string.progress_empty_title),
                message = stringResource(R.string.progress_empty_message),
            )
        }
    }
}
