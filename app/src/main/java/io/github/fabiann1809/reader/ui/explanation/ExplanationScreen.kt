package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun ExplanationScreen(
    onNavigateUp: () -> Unit,
    onNoteSaved: () -> Unit,
    viewModel: ExplanationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saveState) {
        if (uiState.saveState == SaveState.SAVED) onNoteSaved()
    }
    ExplanationContent(
        uiState = uiState,
        onRetry = viewModel::retry,
        onSaveAsNote = viewModel::saveAsNote,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun ExplanationContent(
    uiState: ExplanationUiState,
    onRetry: () -> Unit,
    onSaveAsNote: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.explanation_title), onNavigateUp = onNavigateUp)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(stringResource(R.string.explanation_source_label))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = uiState.sourceText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            SectionTitle(stringResource(R.string.explanation_result_label))
            when (val state = uiState.explanation) {
                ExplanationState.Loading -> Loading()
                // Selectable so the user can copy parts of the explanation.
                is ExplanationState.Success -> {
                    SelectionContainer {
                        Text(text = state.explanation.toPlainText(explanationLabels()), style = MaterialTheme.typography.bodyLarge)
                    }
                    SaveButton(uiState, onSaveAsNote)
                }
                is ExplanationState.Failed -> Failed(state.error, onRetry)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SaveButton(uiState: ExplanationUiState, onSaveAsNote: () -> Unit) {
    Button(onClick = onSaveAsNote, enabled = uiState.canSave, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                if (uiState.saveState == SaveState.SAVED) R.string.explanation_saved_note else R.string.explanation_save_note,
            ),
        )
    }
}

@Composable
private fun Loading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.explanation_loading))
    }
}

@Composable
private fun Failed(error: Throwable, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(aiErrorMessageRes(error)), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) {
            Text(stringResource(R.string.explanation_retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplanationPreview() {
    ReaderTheme {
        ExplanationContent(
            uiState = ExplanationUiState(
                sourceText = "La entropía es una medida del desorden de un sistema.",
                explanation = ExplanationState.Success(
                    Explanation(
                        mainIdea = "El desorden siempre tiende a aumentar.",
                        simpleExplanation = "Las cosas se desordenan solas y no vuelven a ordenarse sin esfuerzo.",
                        analogy = "Como un castillo de arena que las olas deshacen.",
                        keyTerms = listOf(KeyTerm("Entropía", "Medida del desorden de un sistema.")),
                    ),
                ),
            ),
            onRetry = {},
            onSaveAsNote = {},
            onNavigateUp = {},
        )
    }
}
