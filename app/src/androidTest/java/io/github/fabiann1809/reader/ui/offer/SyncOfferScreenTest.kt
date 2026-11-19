package io.github.fabiann1809.reader.ui.offer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.ui.account.ConnectionProblem
import io.github.fabiann1809.reader.ui.account.SyncUiState
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncOfferScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private val calls = mutableListOf<String>()

    private fun show(state: SyncUiState = SyncUiState()) {
        composeRule.setContent {
            ReaderTheme {
                SyncOfferContent(state, onEnable = { calls += "enable" }, onSkip = { calls += "skip" }, onRetry = { calls += "retry" })
            }
        }
    }

    @Test
    fun explainsWhatItDoesAndOffersBothAnswers() {
        show()

        composeRule.onNodeWithText(string(R.string.offer_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.offer_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.offer_later_hint)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.offer_enable)).performClick()
        composeRule.onNodeWithText(string(R.string.offer_skip)).performClick()

        assertEquals(listOf("enable", "skip"), calls)
    }

    @Test
    fun onceTheSyncIsOnItShowsTheLoadingScreenInsteadOfAnEmptyLibrary() {
        show(SyncUiState(status = SyncStatus.Synced(null)))

        composeRule.onNodeWithText(string(R.string.restore_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.offer_title)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.restore_continue)).performClick()

        assertEquals(listOf("skip"), calls)
    }

    @Test
    fun anAccountWithNothingInDriveSeesNoLoadingScreenBecauseThereIsNothingToWaitFor() {
        show(SyncUiState(status = SyncStatus.Synced(null), cloudHasData = false))

        composeRule.onNodeWithText(string(R.string.restore_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.offer_title)).assertDoesNotExist()
    }

    @Test
    fun anAccountWithALibraryInDriveSeesTheLoadingScreen() {
        show(SyncUiState(status = SyncStatus.Synced(null), cloudHasData = true))

        composeRule.onNodeWithText(string(R.string.restore_title)).assertIsDisplayed()
    }

    @Test
    fun ifTheFirstSyncFailsItSaysWhyAndCanRetry() {
        show(SyncUiState(status = SyncStatus.Failed(SyncError.QuotaExceeded())))

        composeRule.onNodeWithText(string(R.string.sync_error_quota)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.restore_retry)).performClick()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun whileConnectingBothButtonsWait() {
        show(SyncUiState(connecting = true))

        composeRule.onNodeWithText(string(R.string.settings_sync_connecting)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.offer_skip)).assertIsNotEnabled()
    }

    @Test
    fun aFailedConnectionIsExplainedAndCanBeTriedAgain() {
        show(SyncUiState(connectionProblem = ConnectionProblem.UNKNOWN))

        composeRule.onNodeWithText(string(R.string.settings_sync_connect_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.offer_enable)).performClick()

        assertEquals(listOf("enable"), calls)
    }
}
