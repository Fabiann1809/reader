package io.github.fabiann1809.reader.ui.privacy

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun explainsWhatIsSentWhereAndWhatStaysOnThePhone() {
        composeRule.setContent { ReaderTheme { PrivacyScreen(onNavigateUp = {}) } }

        composeRule.onNodeWithText(string(R.string.privacy_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.privacy_sent_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.privacy_provider_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.privacy_local_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.privacy_key_title)).performScrollTo().assertIsDisplayed()
    }
}
