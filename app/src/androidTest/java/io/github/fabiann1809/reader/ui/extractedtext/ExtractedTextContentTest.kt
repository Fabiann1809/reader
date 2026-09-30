package io.github.fabiann1809.reader.ui.extractedtext

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtractedTextContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun setContent(
        uiState: ExtractedTextUiState,
        onExplain: (String) -> Unit = {},
        onNavigateUp: () -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme {
                ExtractedTextContent(
                    uiState = uiState,
                    onTextChange = {},
                    onExplain = onExplain,
                    onNavigateUp = onNavigateUp,
                )
            }
        }
    }

    @Test
    fun explainSendsTheReviewedText() {
        var explained: String? = null
        setContent(ExtractedTextUiState.Editing("La entropía mide el desorden."), onExplain = { explained = it })

        composeRule.onNodeWithText(string(R.string.extracted_text_explain)).assertIsEnabled().performClick()

        assertEquals("La entropía mide el desorden.", explained)
    }

    @Test
    fun explainIsDisabledForBlankText() {
        setContent(ExtractedTextUiState.Editing("   "))

        composeRule.onNodeWithText(string(R.string.extracted_text_explain)).assertIsNotEnabled()
    }

    @Test
    fun ocrFailureShowsTheReasonAndGoesBackToTheCamera() {
        var wentBack = false
        setContent(ExtractedTextUiState.Failed(OcrFailure.NO_TEXT_FOUND), onNavigateUp = { wentBack = true })

        composeRule.onNodeWithText(string(R.string.extracted_text_no_text)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.extracted_text_retake)).performClick()

        assertTrue(wentBack)
    }
}
