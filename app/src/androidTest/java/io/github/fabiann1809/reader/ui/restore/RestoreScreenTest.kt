package io.github.fabiann1809.reader.ui.restore

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private val calls = mutableListOf<String>()

    private fun show(state: RestoreState) {
        composeRule.setContent {
            ReaderTheme { RestoreScreen(state, onContinue = { calls += "continue" }, onRetry = { calls += "retry" }) }
        }
    }

    @Test
    fun whileRestoringItSaysSoAndLetsThePersonGoOnWithoutWaiting() {
        show(RestoreState.Restoring)

        composeRule.onNodeWithText(string(R.string.restore_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_wifi_hint)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.restore_continue)).performClick()

        assertEquals(listOf("continue"), calls)
    }

    @Test
    fun aFailureShowsItsReasonWithRetryAndContinue() {
        show(RestoreState.Failed(SyncError.QuotaExceeded()))

        composeRule.onNodeWithText(string(R.string.restore_failed_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.sync_error_quota)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.restore_retry)).performClick()
        composeRule.onNodeWithText(string(R.string.restore_failed_continue)).performClick()

        assertEquals(listOf("retry", "continue"), calls)
    }
}
