package io.github.fabiann1809.reader.ui.review

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.ui.voice.label

@Composable
fun ReviewScreen(
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewContent(uiState, onSelectMode = viewModel::selectMode, onStartSession = onStartSession, modifier = modifier)
}

/**
 * "Repasar" tab (lámina 1h): the chips Hoy · Por libro · Por etiqueta and the cards due today, or
 * the empty state. Cards are made from a book's detail or from notes, explanations and selections.
 */
@Composable
fun ReviewContent(
    uiState: ReviewUiState,
    onSelectMode: (ReviewMode) -> Unit,
    modifier: Modifier = Modifier,
    onStartSession: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.tab_review)) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ModeChips(uiState.mode, onSelectMode)
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.dueCards.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StatusMessage(
                        icon = R.drawable.ic_cards,
                        title = stringResource(R.string.review_empty_title),
                        message = stringResource(R.string.review_empty_message),
                    )
                }
                else -> DueCards(uiState, onStartSession)
            }
        }
    }
}

@Composable
private fun ModeChips(selected: ReviewMode, onSelect: (ReviewMode) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        ReviewMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(mode.label())) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun DueCards(uiState: ReviewUiState, onStartSession: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "count") {
            val count = uiState.dueCards.size
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.review_due_count, count, count),
                    style = MaterialTheme.typography.titleMedium,
                )
                // Design 03 §3.7: "Empezar (12 fichas)".
                PrimaryButton(
                    text = pluralStringResource(R.plurals.review_start, count, count),
                    onClick = onStartSession,
                    icon = R.drawable.ic_cards,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        uiState.groups.forEach { group ->
            if (uiState.mode != ReviewMode.TODAY) {
                item(key = "group:${group.title}:${group.tag}") {
                    val tag = group.tag
                    Text(
                        text = group.title ?: stringResource(tag?.label() ?: R.string.review_no_tag),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            items(group.cards, key = { it.card.id }) { dueCard -> DueCardItem(dueCard) }
        }
    }
}

@StringRes
private fun ReviewMode.label(): Int = when (this) {
    ReviewMode.TODAY -> R.string.review_mode_today
    ReviewMode.BY_BOOK -> R.string.review_mode_by_book
    ReviewMode.BY_TAG -> R.string.review_mode_by_tag
}
