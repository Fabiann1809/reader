package io.github.fabiann1809.reader.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun startsOnHome() {
        composeRule.onNodeWithText(string(R.string.welcome_message)).assertIsDisplayed()
    }

    @Test
    fun navigatesToSettingsAndBack() {
        composeRule.onNodeWithContentDescription(string(R.string.settings_title)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_placeholder)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.navigate_up)).performClick()
        composeRule.onNodeWithText(string(R.string.welcome_message)).assertIsDisplayed()
    }
}
