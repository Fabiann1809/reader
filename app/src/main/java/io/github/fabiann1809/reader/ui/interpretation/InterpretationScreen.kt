package io.github.fabiann1809.reader.ui.interpretation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.AiButton
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.aiErrorMessageRes
import io.github.fabiann1809.reader.ui.components.readerTextFieldColors
import io.github.fabiann1809.reader.ui.components.readerTextFieldShape
import io.github.fabiann1809.reader.ui.explanation.AiGeneratedChip
import io.github.fabiann1809.reader.ui.noteeditor.Quote
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun InterpretationScreen(
    onNavigateUp: () -> Unit,
    onQuiz: (source: String, count: Int, title: String) -> Unit = { _, _, _ -> },
    viewModel: InterpretationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InterpretationContent(
        uiState = uiState,
        onOwnWordsChange = viewModel::onOwnWordsChange,
        onAnalyze = viewModel::analyze,
        onNavigateUp = onNavigateUp,
        onQuiz = { title -> onQuiz(uiState.sourceText, PARAGRAPH_QUIZ_SIZE, title) },
    )
}

/** "Ahora tú" (lámina 1i): the text, the reader's own words, "Analizar" and the AI's analysis. */
@Composable
fun InterpretationContent(
    uiState: InterpretationUiState,
    onOwnWordsChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    onQuiz: (title: String) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.interpretation_title), onNavigateUp = onNavigateUp) },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Quote(uiState.sourceText)
            Text(stringResource(R.string.interpretation_prompt), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                shape = readerTextFieldShape,
                colors = readerTextFieldColors(),
                value = uiState.ownWords,
                onValueChange = onOwnWordsChange,
                label = { Text(stringResource(R.string.interpretation_field)) },
                minLines = OWN_WORDS_MIN_LINES,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            AiButton(
                text = stringResource(R.string.interpretation_analyze),
                onClick = onAnalyze,
                enabled = uiState.canAnalyze,
                modifier = Modifier.fillMaxWidth(),
            )
            AnalysisSection(uiState.analysis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.analysis is AnalysisState.Done) {
                    val title = stringResource(R.string.quiz_title_paragraph)
                    AiButton(
                        text = stringResource(R.string.quiz_try_me),
                        onClick = { onQuiz(title) },
                        icon = null,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlineButton(
                    text = stringResource(R.string.interpretation_close),
                    onClick = onNavigateUp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AnalysisSection(analysis: AnalysisState) {
    when (analysis) {
        AnalysisState.Idle -> Unit
        AnalysisState.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ReaderTheme.colors.ai)
            Text(stringResource(R.string.interpretation_analyzing), style = MaterialTheme.typography.bodyMedium)
        }
        is AnalysisState.Failed -> Text(
            text = stringResource(aiErrorMessageRes(analysis.error)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        is AnalysisState.Done -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.interpretation_analysis),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AiGeneratedChip()
            }
            AnalysisBlocks(analysis.analysis)
        }
    }
}

private const val OWN_WORDS_MIN_LINES = 5

// One paragraph is enough for a short quiz only.
private const val PARAGRAPH_QUIZ_SIZE = 3

@Preview(showBackground = true)
@Composable
private fun InterpretationPreview() {
    ReaderTheme {
        InterpretationContent(
            uiState = InterpretationUiState(
                sourceText = "La entropía es una medida del desorden de un sistema.",
                ownWords = "Que las cosas tienden a desordenarse.",
                analysis = AnalysisState.Done(
                    InterpretationAnalysis(
                        understood = "Captas la idea del desorden.",
                        incomplete = "Falta que ocurre en un sistema aislado.",
                    ),
                ),
            ),
            onOwnWordsChange = {},
            onAnalyze = {},
            onNavigateUp = {},
        )
    }
}
