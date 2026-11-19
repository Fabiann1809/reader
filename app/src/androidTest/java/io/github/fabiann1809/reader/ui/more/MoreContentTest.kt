package io.github.fabiann1809.reader.ui.more

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.account.AccountUiState
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoreContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private val opened = mutableListOf<MoreEntry>()
    private var signOuts = 0

    private fun show(accountState: AccountUiState = AccountUiState()) {
        composeRule.setContent {
            ReaderTheme {
                MoreContent(MoreUiState(), onOpen = { opened += it }, accountState = accountState, onSignOut = { signOuts++ })
            }
        }
    }

    @Test
    fun hasAnAccountEntryThatOpensIt() {
        show()

        composeRule.onNodeWithText(string(R.string.account_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.more_account_hint)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.account_title)).performClick()

        assertEquals(listOf(MoreEntry.ACCOUNT), opened)
    }

    @Test
    fun signsOutFromTheBottomWithTheSyncOff() {
        show(AccountUiState(syncOn = false))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out)).performScrollTo().performClick()

        assertEquals(1, signOuts)
    }

    @Test
    fun withTheSyncOnItAsksBeforeSigningOut() {
        show(AccountUiState(syncOn = true))

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_confirm_body)).assertIsDisplayed()
        assertEquals(0, signOuts)

        composeRule.onNodeWithText(string(R.string.settings_account_sign_out_confirm)).performClick()

        assertEquals(1, signOuts)
    }
}
