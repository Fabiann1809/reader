package io.github.fabiann1809.reader.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.sync.SyncStatus
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncSectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    private val calls = mutableListOf<String>()

    private fun show(state: SyncUiState) {
        composeRule.setContent {
            ReaderTheme {
                SyncSectionContent(
                    state = state,
                    onEnabled = { calls += "enabled:$it" },
                    onWifiOnly = { calls += "wifiOnly:$it" },
                    onSyncNow = { calls += "syncNow" },
                    onRetry = { calls += "retry" },
                )
            }
        }
    }

    @Test
    fun whenItIsOffOnlyTheSwitchShowsAndTurnsItOn() {
        show(SyncUiState(status = SyncStatus.Disabled))

        composeRule.onNodeWithText(string(R.string.settings_sync_toggle)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sync_now)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.settings_sync_toggle)).performClick()

        assertEquals(listOf("enabled:true"), calls)
    }

    @Test
    fun whenItIsOnItShowsTheStatusTheWifiOptionAndSyncNow() {
        show(SyncUiState(status = SyncStatus.Synced(null), wifiOnly = true))

        composeRule.onNodeWithText(string(R.string.sync_status_synced)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sync_wifi)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.settings_sync_wifi)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_sync_now)).performClick()

        assertEquals(listOf("wifiOnly:false", "syncNow"), calls)
    }

    @Test
    fun theLastSyncIsShownAsATime() {
        show(SyncUiState(status = SyncStatus.Synced(System.currentTimeMillis())))

        composeRule.onNodeWithText(
            string(R.string.sync_status_synced_at, string(R.string.sync_just_now)),
        ).assertIsDisplayed()
    }

    @Test
    fun whileSyncingTheButtonWaits() {
        show(SyncUiState(status = SyncStatus.Syncing))

        composeRule.onNodeWithText(string(R.string.sync_status_syncing)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sync_now)).assertIsNotEnabled()
    }

    @Test
    fun pendingChangesAreExplained() {
        show(SyncUiState(status = SyncStatus.Pending))

        composeRule.onNodeWithText(string(R.string.sync_status_pending)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sync_now)).assertIsEnabled()
    }

    @Test
    fun anErrorShowsItsMessageAndRetryInsteadOfSyncNow() {
        show(SyncUiState(status = SyncStatus.Failed(SyncError.QuotaExceeded())))

        composeRule.onNodeWithText(string(R.string.sync_error_quota)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sync_now)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.settings_sync_retry)).performClick()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun aLostPermissionSaysToConnectAgain() {
        show(SyncUiState(status = SyncStatus.Failed(SyncError.NotAuthorized())))

        composeRule.onNodeWithText(string(R.string.sync_error_not_authorized)).assertIsDisplayed()
    }

    @Test
    fun whileConnectingItSaysSoAndIgnoresTheSwitch() {
        show(SyncUiState(status = SyncStatus.Disabled, connecting = true))

        composeRule.onNodeWithText(string(R.string.settings_sync_connecting)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_sync_toggle)).performClick()

        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun aConnectionFailureIsShown() {
        show(SyncUiState(status = SyncStatus.Disabled, connectionFailed = true))

        composeRule.onNodeWithText(string(R.string.settings_sync_connect_failed)).assertIsDisplayed()
    }
}
