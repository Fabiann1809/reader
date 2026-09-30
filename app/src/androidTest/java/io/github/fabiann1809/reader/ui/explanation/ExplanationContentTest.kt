package io.github.fabiann1809.reader.ui.explanation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiError
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
    fun showsSourceTextAndLoadingMessage() {
        setContent(ExplanationState.Loading)

        composeRule.onNodeWithText(sourceText).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.explanation_loading)).assertIsDisplayed()
    }

    @Test
    fun showsTheExplanation() {
        setContent(ExplanationState.Success("Idea central: el desorden aumenta."))

        composeRule.onNodeWithText(sourceText).assertIsDisplayed()
        composeRule.onNodeWithText("Idea central: el desorden aumenta.").assertIsDisplayed()
    }

    @Test
    fun saveButtonSavesTheExplanation() {
        var saved = false
        setContent(ExplanationState.Success("Idea central"), onSaveAsNote = { saved = true })

        composeRule.onNodeWithText(string(R.string.explanation_save_note)).performClick()
        assertTrue(saved)
    }

    @Test
    fun saveButtonIsDisabledOnceSaved() {
        setContent(ExplanationState.Success("Idea central"), saveState = SaveState.SAVED)

        composeRule.onNodeWithText(string(R.string.explanation_saved_note)).assertIsNotEnabled()
    }

    @Test
    fun showsTheErrorMessageWithRetry() {
        var retried = false
        setContent(ExplanationState.Failed(AiError.NoInternet(RuntimeException())), onRetry = { retried = true })

        composeRule.onNodeWithText(string(R.string.ai_error_no_internet)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.explanation_retry)).performClick()
        assertTrue(retried)
    }
}
