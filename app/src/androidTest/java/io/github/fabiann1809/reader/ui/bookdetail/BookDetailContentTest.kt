package io.github.fabiann1809.reader.ui.bookdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val book = Book(id = 1, title = "Cosmos", author = "Carl Sagan")

    private fun setContent(notes: List<Note>) {
        composeRule.setContent {
            ReaderTheme {
                BookDetailContent(
                    uiState = BookDetailUiState.Success(book, notes),
                    onNavigateUp = {},
                    onUpdateProgress = { _, _ -> },
                    onDeleteBook = {},
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
}
