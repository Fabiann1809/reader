package io.github.fabiann1809.reader.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderControlsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun state(visible: Boolean) = ReaderUiState.Ready(
        bookId = 1,
        format = BookFormat.EPUB,
        title = "El principito",
        chapter = "Capítulo 4",
        progression = 0.4f,
        controlsVisible = visible,
    )

    @Test
    fun visibleControlsShowTheTitleChapterAndActions() {
        composeRule.setContent { ReaderTheme { ReaderControls(state(visible = true), onBack = {}, onSeek = {}) } }

        composeRule.onNodeWithText("El principito").assertIsDisplayed()
        composeRule.onNodeWithText("Capítulo 4").assertIsDisplayed()
        listOf(
            R.string.reader_index,
            R.string.reader_text_settings_description,
            R.string.reader_voice_description,
            R.string.reader_ai_description,
            R.string.reader_record_description,
            R.string.reader_bookmark,
            R.string.reader_progress,
        ).forEach { composeRule.onNodeWithContentDescription(string(it)).assertIsDisplayed() }
    }

    @Test
    fun hiddenControlsLeaveOnlyThePage() {
        composeRule.setContent { ReaderTheme { ReaderControls(state(visible = false), onBack = {}, onSeek = {}) } }

        composeRule.onNodeWithText("El principito").assertDoesNotExist()
    }

    @Test
    fun backLeavesTheReader() {
        var left = false
        composeRule.setContent { ReaderTheme { ReaderControls(state(visible = true), onBack = { left = true }, onSeek = {}) } }

        composeRule.onNodeWithContentDescription(string(R.string.navigate_up)).performClick()

        assertTrue(left)
    }
}
