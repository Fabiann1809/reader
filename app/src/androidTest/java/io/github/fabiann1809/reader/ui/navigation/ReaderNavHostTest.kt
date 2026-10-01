package io.github.fabiann1809.reader.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Before
    fun setUp() {
        composeRule.setContent {
            ReaderTheme { ReaderNavHost() }
        }
    }

    @Test
    fun startsOnLibrary() {
        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun navigatesToSettingsAndBack() {
        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_guide_title)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.navigate_up)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun opensPrivacyFromSettings() {
        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_privacy)).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.privacy_sent_title)).assertIsDisplayed()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    @Test
    fun bottomBarSwitchesBetweenTabs() {
        composeRule.onNodeWithText(string(R.string.tab_review)).performClick()
        composeRule.onNodeWithText(string(R.string.review_empty_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.tab_progress)).performClick()
        composeRule.onNodeWithText(string(R.string.progress_empty_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.tab_more)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun backFromAnyTabReturnsToLibrary() {
        composeRule.onNodeWithText(string(R.string.tab_review)).performClick()
        composeRule.onNodeWithText(string(R.string.tab_progress)).performClick()

        pressBack()

        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun bottomBarIsHiddenOnInnerScreens() {
        composeRule.onNodeWithText(string(R.string.tab_more)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_title)).performClick()

        composeRule.onNodeWithText(string(R.string.tab_review)).assertDoesNotExist()
    }
}
