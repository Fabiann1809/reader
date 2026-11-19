package io.github.fabiann1809.reader.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
class AccountSectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private var signOuts = 0

    private fun show(state: AccountUiState) {
        composeRule.setContent {
            ReaderTheme { AccountSection("ana@example.com", state) { signOuts++ } }
        }
    }

    @Test
    fun withTheSyncOffItSignsOutAtOnce() {
        show(AccountUiState(syncOn = false))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out)).performClick()

        assertEquals(1, signOuts)
    }

    @Test
    fun withTheSyncOnItAsksBeforeEmptyingThePhone() {
        show(AccountUiState(syncOn = true))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out)).performClick()

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_confirm_body)).assertIsDisplayed()
        assertEquals(0, signOuts)

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_confirm)).performClick()

        assertEquals(1, signOuts)
    }

    @Test
    fun cancellingTheConfirmationKeepsTheSession() {
        show(AccountUiState(syncOn = true))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out)).performClick()
        composeRule.onNodeWithText(string(R.string.action_cancel)).performClick()

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_confirm_body)).assertDoesNotExist()
        assertEquals(0, signOuts)
    }

    @Test
    fun whileSavingItSaysSoAndTheButtonWaits() {
        show(AccountUiState(syncOn = true, signingOut = true))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_saving)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_account_sign_out)).assertIsNotEnabled()
    }

    @Test
    fun aFailedSaveExplainsWhyTheSessionWasKept() {
        show(AccountUiState(syncOn = true, saveFailed = true))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_failed)).assertIsDisplayed()
    }
}
