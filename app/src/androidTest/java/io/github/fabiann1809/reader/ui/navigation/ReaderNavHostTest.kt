package io.github.fabiann1809.reader.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
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

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Only the library has a search button or, when it is empty, "Añadir libro". The device's real
     * books decide which one shows (and the add button says "Continuar" once a book was opened).
     */
    private fun assertOnLibrary() {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithContentDescription(string(R.string.library_search)).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(string(R.string.library_add_book)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openMore(entry: Int) {
        composeRule.onNodeWithText(string(R.string.tab_more)).performClick()
        composeRule.onNodeWithText(string(entry)).performClick()
    }

    @Test
    fun startsOnLibrary() {
        assertOnLibrary()
    }

    @Test
    fun bottomBarSwitchesBetweenTabs() {
        composeRule.onNodeWithText(string(R.string.tab_review)).performClick()
        // The device may hold cards or not: the "Hoy" chip shows either way.
        composeRule.onNodeWithText(string(R.string.review_mode_today)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.tab_progress)).performClick()
        // The device may have reading sessions or not: the tab's title shows either way (and its tab).
        composeRule.onAllNodesWithText(string(R.string.tab_progress)).assertCountEquals(2)

        composeRule.onNodeWithText(string(R.string.tab_more)).performClick()
        composeRule.onNodeWithText(string(R.string.all_notes_title)).assertIsDisplayed()
    }

    @Test
    fun backFromAnyTabReturnsToLibrary() {
        composeRule.onNodeWithText(string(R.string.tab_review)).performClick()
        composeRule.onNodeWithText(string(R.string.tab_progress)).performClick()

        pressBack()

        assertOnLibrary()
    }

    @Test
    fun opensSettingsFromMoreAndComesBack() {
        openMore(R.string.settings_title)
        // The first section; the API key's guide is further down since T17.1.
        composeRule.onNodeWithText(string(R.string.settings_appearance)).assertIsDisplayed()
        // The bottom bar is hidden on inner screens.
        composeRule.onNodeWithText(string(R.string.tab_review)).assertDoesNotExist()

        composeRule.onNodeWithContentDescription(string(R.string.navigate_up)).performClick()
        composeRule.onNodeWithText(string(R.string.all_notes_title)).assertIsDisplayed()
    }

    @Test
    fun opensPrivacyFromSettings() {
        openMore(R.string.settings_title)
        composeRule.onNodeWithText(string(R.string.settings_privacy)).performScrollTo().performClick()

        composeRule.onNodeWithText(string(R.string.privacy_sent_title)).assertIsDisplayed()
    }

    @Test
    fun opensEveryOtherEntryOfMore() {
        openMore(R.string.all_notes_title)
        composeRule.onNodeWithText(string(R.string.all_notes_title)).assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText(string(R.string.backup_title)).performClick()
        composeRule.onNodeWithText(string(R.string.backup_coming_title)).assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText(string(R.string.settings_privacy)).performClick()
        composeRule.onNodeWithText(string(R.string.privacy_sent_title)).assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText(string(R.string.about_title)).performClick()
        composeRule.onNodeWithText(string(R.string.about_licenses)).assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
