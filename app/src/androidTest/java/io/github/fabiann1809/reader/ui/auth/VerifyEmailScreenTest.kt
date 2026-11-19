package io.github.fabiann1809.reader.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.auth.AuthError
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerifyEmailScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private val calls = mutableListOf<String>()

    private fun show(state: VerifyEmailUiState = VerifyEmailUiState(email = "ana@example.com")) {
        composeRule.setContent {
            ReaderTheme {
                VerifyEmailContent(
                    state,
                    onCheck = { calls += "check" },
                    onResend = { calls += "resend" },
                    onUseAnotherEmail = { calls += "other" },
                )
            }
        }
    }

    @Test
    fun saysWhereTheLinkWent() {
        show()

        composeRule.onNodeWithText(string(R.string.verify_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.verify_body, "ana@example.com")).assertIsDisplayed()
    }

    @Test
    fun eachButtonDoesItsThing() {
        show()

        composeRule.onNodeWithText(string(R.string.verify_check)).performClick()
        composeRule.onNodeWithText(string(R.string.verify_resend)).performClick()
        composeRule.onNodeWithText(string(R.string.verify_other_email)).performClick()

        assertEquals(listOf("check", "resend", "other"), calls)
    }

    @Test
    fun saysWhenTheAddressIsStillNotConfirmed() {
        show(VerifyEmailUiState(email = "ana@example.com", stillPending = true))

        composeRule.onNodeWithText(string(R.string.verify_still_pending)).assertIsDisplayed()
    }

    @Test
    fun confirmsThatTheLinkWasSentAgain() {
        show(VerifyEmailUiState(email = "ana@example.com", resent = true))

        composeRule.onNodeWithText(string(R.string.verify_resent)).assertIsDisplayed()
    }

    @Test
    fun showsWhyItFailed() {
        show(VerifyEmailUiState(email = "ana@example.com", error = AuthError.TooManyAttempts()))

        composeRule.onNodeWithText(string(R.string.auth_error_too_many)).assertIsDisplayed()
    }
}
