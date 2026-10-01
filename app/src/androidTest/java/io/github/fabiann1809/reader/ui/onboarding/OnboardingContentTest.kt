package io.github.fabiann1809.reader.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var finishCount = 0

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun setContent() {
        composeRule.setContent { ReaderTheme { OnboardingContent(onFinish = { finishCount++ }) } }
    }

    @Test
    fun nextWalksThroughTheThreeStepsAndStartFinishes() {
        setContent()
        composeRule.onNodeWithText(string(R.string.onboarding_shelves_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.onboarding_next)).performClick()
        composeRule.onNodeWithText(string(R.string.onboarding_understand_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.onboarding_next)).performClick()
        composeRule.onNodeWithText(string(R.string.onboarding_remember_title)).assertIsDisplayed()
        // No "skip" on the last step: "Empezar" already finishes.
        composeRule.onNodeWithText(string(R.string.onboarding_skip)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.onboarding_start)).performClick()
        assertEquals(1, finishCount)
    }

    @Test
    fun skipFinishesRightAway() {
        setContent()

        composeRule.onNodeWithText(string(R.string.onboarding_skip)).performClick()

        assertEquals(1, finishCount)
    }
}
