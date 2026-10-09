package io.github.fabiann1809.reader.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.remember
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import io.github.fabiann1809.reader.util.AppLocale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.StatusMessage

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressContent(uiState, onGoalChange = viewModel::setGoal, onCelebrated = viewModel::celebrated, modifier = modifier)
}

/** "Progreso" tab (lámina 1i): the goal and streak, this week, and the books finished this year. */
@Composable
fun ProgressContent(
    uiState: ProgressUiState,
    modifier: Modifier = Modifier,
    onGoalChange: (ReadingGoal) -> Unit = {},
    onCelebrated: () -> Unit = {},
) {
    var editingGoal by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        ProgressHeader()
        val stats = uiState.stats
        val content = Modifier.weight(1f).fillMaxWidth()
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = content
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = NAV_BAR_CLEARANCE),
            ) {
                GoalCard(stats, onEditGoal = { editingGoal = true })
                StreakCard(stats, celebrate = uiState.celebrate, onCelebrated = onCelebrated)
                WeekCard(stats)
                FinishedBooksCard(stats)
            }
        }
    }
    val goal = uiState.stats?.goal
    if (editingGoal && goal != null) {
        GoalDialog(
            goal = goal,
            onSave = { newGoal ->
                onGoalChange(newGoal)
                editingGoal = false
            },
            onDismiss = { editingGoal = false },
        )
    }
}

/** "Progreso" and today's date. */
@Composable
private fun ProgressHeader() {
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", AppLocale)) }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
        Text(stringResource(R.string.tab_progress), style = MaterialTheme.typography.displaySmall)
        Text(
            text = today.replaceFirstChar { it.titlecase(AppLocale) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Room for the floating bottom bar.
private val NAV_BAR_CLEARANCE = 120.dp
