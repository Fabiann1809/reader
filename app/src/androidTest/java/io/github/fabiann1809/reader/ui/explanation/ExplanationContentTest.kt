package io.github.fabiann1809.reader.ui.explanation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplanationContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sourceText = "La entropía es una medida del desorden de un sistema."
    private val explanation = Explanation(
        mainIdea = "El desorden siempre aumenta.",
        simpleExplanation = "Las cosas tienden a desordenarse solas.",
        analogy = "Como un cuarto que se desordena si nadie lo recoge.",
        keyTerms = listOf(KeyTerm("Entropía", "Medida del desorden.")),
    )

    private fun setContent(
        explanation: ExplanationState,
        saveState: SaveState = SaveState.NOT_SAVED,
        onRetry: () -> Unit = {},
        onSaveAsNote: () -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme {
                ExplanationContent(
                    uiState = ExplanationUiState(sourceText, explanation, saveState),
                    onRetry = onRetry,
                    onSaveAsNote = onSaveAsNote,
                    onNavigateUp = {},
                )
            }
        }
    }

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun showsLoadingWithTheSourceCollapsed() {
        setContent(ExplanationState.Loading)

        composeRule.onNodeWithText(string(R.string.explanation_loading)).assertIsDisplayed()
        composeRule.onNodeWithText(sourceText).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.explanation_source_label)).performClick()
        composeRule.onNodeWithText(sourceText).assertIsDisplayed()
    }

    @Test
    fun showsEachBlockAndTheAiLabel() {
        setContent(ExplanationState.Success(explanation))

        composeRule.onNodeWithText(string(R.string.explanation_ai_label)).assertIsDisplayed()
        composeRule.onNodeWithText(explanation.mainIdea).assertIsDisplayed()
        composeRule.onNodeWithText(explanation.simpleExplanation).assertIsDisplayed()
        composeRule.onNodeWithText(explanation.analogy).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.explanation_block_main_idea)).assertIsDisplayed()
    }

    @Test
    fun tappingAKeyTermShowsItsDefinition() {
        setContent(ExplanationState.Success(explanation))

        composeRule.onNodeWithText("Medida del desorden.").assertDoesNotExist()
        composeRule.onNodeWithText("Entropía").performScrollTo().performClick()
        composeRule.onNodeWithText("Medida del desorden.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun showsTheCaveatWhenThereIsOne() {
        setContent(ExplanationState.Success(explanation.copy(caveat = "El fragmento está cortado.")))

        composeRule.onNodeWithText("El fragmento está cortado.").assertIsDisplayed()
    }

    @Test
    fun saveButtonSavesTheExplanation() {
        var saved = false
        setContent(ExplanationState.Success(explanation), onSaveAsNote = { saved = true })

        composeRule.onNodeWithText(string(R.string.explanation_save_note)).performScrollTo().performClick()
        assertTrue(saved)
    }

    @Test
    fun saveButtonIsDisabledOnceSaved() {
        setContent(ExplanationState.Success(explanation), saveState = SaveState.SAVED)

        composeRule.onNodeWithText(string(R.string.explanation_saved_note)).assertIsNotEnabled()
    }

    @Test
    fun offlineErrorShowsItsTitleAndRetry() {
        var retried = false
        setContent(ExplanationState.Failed(AiError.NoInternet(RuntimeException())), onRetry = { retried = true })

        composeRule.onNodeWithText(string(R.string.explanation_offline_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.ai_error_no_internet)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.explanation_retry)).performClick()
        assertTrue(retried)
    }
}
