package io.github.fabiann1809.reader.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonsLoadingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val spinner = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    @Test
    fun aLoadingButtonShowsASpinnerAndIgnoresTaps() {
        var taps = 0
        composeRule.setContent { ReaderTheme { AiButton(text = "Proponer", onClick = { taps++ }, loading = true) } }

        composeRule.onNode(spinner).assertIsDisplayed()
        composeRule.onNodeWithText("Proponer").performClick()

        assertEquals(0, taps)
    }

    @Test
    fun aButtonThatIsNotLoadingHasNoSpinnerAndWorks() {
        var taps = 0
        composeRule.setContent { ReaderTheme { PrimaryButton(text = "Analizar", onClick = { taps++ }) } }

        composeRule.onNode(spinner).assertDoesNotExist()
        composeRule.onNodeWithText("Analizar").performClick()

        assertEquals(1, taps)
    }
}
