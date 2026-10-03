package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressContent(uiState, modifier)
}

/** "Progreso" tab (lámina 1i): the goal and streak, this week, and the books finished this year. */
@Composable
fun ProgressContent(uiState: ProgressUiState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.tab_progress)) },
    ) { innerPadding ->
        val stats = uiState.stats
        val content = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            stats == null -> Box(content, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.isEmpty -> Box(content, contentAlignment = Alignment.Center) {
                StatusMessage(
                    icon = R.drawable.ic_chart_donut,
                    title = stringResource(R.string.progress_empty_title),
                    message = stringResource(R.string.progress_empty_message),
                )
            }
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = content
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            ) {
                GoalCard(stats)
                WeekCard(stats)
                FinishedBooksCard(stats)
            }
        }
    }
}
