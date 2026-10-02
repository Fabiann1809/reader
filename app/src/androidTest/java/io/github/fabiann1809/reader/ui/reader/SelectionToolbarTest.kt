package io.github.fabiann1809.reader.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class SelectionToolbarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private val selection = TextSelection("memoria", SelectionBounds(left = 200f, top = 900f, right = 400f, bottom = 960f))

    @Test
    fun theCapsuleShowsExplainAndEveryAction() {
        composeRule.setContent {
            ReaderTheme { SelectionToolbar(selection, darkPage = false, SelectionActions(onCopy = {}, onSearch = {}, onShare = {})) }
        }

        composeRule.onNodeWithText(string(R.string.selection_explain)).assertIsDisplayed()
        listOf(R.string.selection_highlight, R.string.selection_note, R.string.selection_voice_note, R.string.selection_card, R.string.selection_more)
            .forEach { composeRule.onNodeWithContentDescription(string(it)).assertIsDisplayed() }
    }

    @Test
    fun moreOffersCopySearchAndShare() {
        val used = mutableListOf<String>()
        composeRule.setContent {
            ReaderTheme {
                SelectionToolbar(
                    selection,
                    darkPage = true,
                    SelectionActions(onCopy = { used += "copy" }, onSearch = { used += "search" }, onShare = { used += "share" }),
                )
            }
        }

        composeRule.onNodeWithContentDescription(string(R.string.selection_more)).performClick()
        composeRule.onNodeWithText(string(R.string.selection_search)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.selection_share)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.selection_copy)).performClick()

        assertEquals(listOf("copy"), used)
    }
}
