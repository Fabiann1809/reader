package io.github.fabiann1809.reader.ui.newcollection

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewCollectionContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dune = Book(id = 1, title = "Dune", author = "F. Herbert")

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun setContent(
        state: NewCollectionUiState,
        onSave: () -> Unit = {},
        onToggleBook: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme {
                NewCollectionContent(
                    uiState = state,
                    onClose = {},
                    onSave = onSave,
                    onNameChange = {},
                    onColorChange = {},
                    onToggleBook = onToggleBook,
                )
            }
        }
    }

    @Test
    fun showsThePreviewAndTheBooksToChoose() {
        setContent(NewCollectionUiState(books = listOf(dune)))

        composeRule.onNodeWithText(string(R.string.new_collection_unnamed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.new_collection_no_books)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.new_collection_pick_books)).assertIsDisplayed()
    }

    @Test
    fun tappingACoverChecksItsBook() {
        var toggled: Long? = null
        setContent(NewCollectionUiState(books = listOf(dune)), onToggleBook = { toggled = it })

        composeRule.onNodeWithText("Dune").performClick()

        assertEquals(1L, toggled)
    }

    @Test
    fun savesOnlyWithANameAndABook() {
        var saved = 0
        setContent(NewCollectionUiState(books = listOf(dune), name = "Verano"), onSave = { saved++ })
        composeRule.onNodeWithText(string(R.string.new_collection_save)).performClick()
        assertEquals(0, saved)
    }

    @Test
    fun savesWhenReady() {
        var saved = 0
        setContent(
            NewCollectionUiState(books = listOf(dune), name = "Verano", selectedIds = listOf(1)),
            onSave = { saved++ },
        )

        composeRule.onNodeWithText(string(R.string.new_collection_save)).performClick()

        assertEquals(1, saved)
    }
}
