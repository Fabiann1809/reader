package io.github.fabiann1809.reader.ui.bookdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val book = Book(id = 1, title = "Cosmos", author = "Carl Sagan")

    private fun setContent(notes: List<Note>, onCapturePage: () -> Unit = {}, onDeleteBook: () -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                BookDetailContent(
                    uiState = BookDetailUiState.Success(book, notes),
                    onNavigateUp = {},
                    onUpdateProgress = { _, _ -> },
                    onDeleteBook = onDeleteBook,
                    onAddNote = {},
                    onNoteClick = {},
                    onCapturePage = onCapturePage,
                )
            }
        }
    }

    @Test
    fun showsEmptyNotesMessage() {
        setContent(notes = emptyList())

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notes_empty)).assertIsDisplayed()
    }

    @Test
    fun showsNotesWithPageAndType() {
        setContent(
            notes = listOf(
                Note(id = 1, bookId = 1, page = 12, content = "Somos polvo de estrellas"),
                Note(id = 2, bookId = 1, content = "Idea central", type = NoteType.EXPLANATION),
            ),
        )

        composeRule.onNodeWithText("Somos polvo de estrellas").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.note_page, 12)).assertIsDisplayed()
        composeRule.onNodeWithText("Idea central").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.note_type_explanation))
            .assertIsDisplayed()
    }

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun captureIsThePrimaryAction() {
        var captured = false
        setContent(notes = emptyList(), onCapturePage = { captured = true })

        composeRule.onNodeWithText(string(R.string.capture_title)).performClick()

        assertTrue(captured)
    }

    @Test
    fun deletingFromTheMenuAsksForConfirmation() {
        var deleted = false
        setContent(notes = emptyList(), onDeleteBook = { deleted = true })

        composeRule.onNodeWithContentDescription(string(R.string.more_options)).performClick()
        composeRule.onNodeWithText(string(R.string.delete_book_title)).performClick()
        assertFalse(deleted)

        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertTrue(deleted)
    }

    @Test
    fun missingBookOffersAWayBack() {
        var wentBack = false
        composeRule.setContent {
            ReaderTheme {
                BookDetailContent(
                    uiState = BookDetailUiState.NotFound,
                    onNavigateUp = { wentBack = true },
                    onUpdateProgress = { _, _ -> },
                    onDeleteBook = {},
                    onAddNote = {},
                    onNoteClick = {},
                    onCapturePage = {},
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.book_not_found)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.book_back_to_library)).performClick()
        assertTrue(wentBack)
    }
}
