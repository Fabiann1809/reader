package io.github.fabiann1809.reader.ui.interpretation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterpretationContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(uiState: InterpretationUiState, onAnalyze: () -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                InterpretationContent(
                    uiState = uiState,
                    onOwnWordsChange = {},
                    onAnalyze = onAnalyze,
                    onNavigateUp = {},
                )
            }
        }
    }

    @Test
    fun analyzeIsDisabledUntilThereAreOwnWords() {
        setContent(InterpretationUiState(sourceText = "Texto"))
        composeRule.onNodeWithText("Analizar").assertIsNotEnabled()
    }

    @Test
    fun analyzeCallsBack() {
        var analyzed = false
        setContent(InterpretationUiState(sourceText = "Texto", ownWords = "Mi idea"), onAnalyze = { analyzed = true })
        composeRule.onNodeWithText("Analizar").performClick()
        assertTrue(analyzed)
    }

    @Test
    fun showsOnlyTheBlocksTheAnalysisHas() {
        val analysis = InterpretationAnalysis(understood = "Captas la idea.", incomplete = "Falta el contexto.")
        setContent(InterpretationUiState("Texto", "Mi idea", AnalysisState.Done(analysis)))
        composeRule.onNodeWithText("Bien entendido").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Incompleto").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Confuso").assertDoesNotExist()
    }

}
