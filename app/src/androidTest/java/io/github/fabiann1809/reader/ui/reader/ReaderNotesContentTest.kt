package io.github.fabiann1809.reader.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderNotesContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun setContent(
        state: ReaderNotesUiState,
        onGoTo: (String) -> Unit = {},
        onOpenNote: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme { ReaderNotesContent(state, onGoTo = onGoTo, onOpenNote = onOpenNote, onDismiss = {}) }
        }
    }

    @Test
    fun showsAMessageWhenThereAreNoNotes() {
        setContent(ReaderNotesUiState(isLoading = false))

        composeRule.onNodeWithText(string(R.string.reader_notes_empty_message)).assertIsDisplayed()
    }

    @Test
    fun aNoteWithoutAPlaceOpensItsEditor() {
        var opened: Long? = null
        setContent(
            ReaderNotesUiState(notes = listOf(Note(id = 4, bookId = 1, content = "Una idea")), isLoading = false),
            onOpenNote = { opened = it },
        )

        composeRule.onNodeWithText("Una idea").performClick()

        assertEquals(4L, opened)
    }

    @Test
    fun aHighlightGoesToItsPlace() {
        var place: String? = null
        setContent(
            ReaderNotesUiState(
                highlights = listOf(Highlight(id = 1, bookId = 1, location = "{\"href\":\"a\"}", text = "Subrayado")),
                isLoading = false,
            ),
            onGoTo = { place = it },
        )

        composeRule.onNodeWithText("Subrayado").performClick()

        assertEquals("{\"href\":\"a\"}", place)
    }
}
