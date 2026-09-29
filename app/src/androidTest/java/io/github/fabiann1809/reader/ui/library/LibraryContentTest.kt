package io.github.fabiann1809.reader.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(uiState: LibraryUiState) {
        composeRule.setContent {
            ReaderTheme { LibraryContent(uiState = uiState, onOpenSettings = {}) }
        }
    }

    @Test
    fun showsEmptyStateWhenThereAreNoBooks() {
        setContent(LibraryUiState(isLoading = false))

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.library_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun showsBooksWithStatusAndProgress() {
        val book = Book(
            id = 1,
            title = "Cosmos",
            author = "Carl Sagan",
            currentPage = 120,
            totalPages = 400,
            status = BookStatus.READING,
        )

        setContent(LibraryUiState(books = listOf(book), isLoading = false))

        composeRule.onNodeWithText("Cosmos").assertIsDisplayed()
        composeRule.onNodeWithText("Carl Sagan").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.book_status_reading))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.book_progress_with_total, 120, 400))
            .assertIsDisplayed()
    }
}
