package io.github.fabiann1809.reader.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(uiState: LibraryUiState) {
        composeRule.setContent {
            ReaderTheme { LibraryContent(uiState = uiState, onBookClick = {}, onAddBook = {}, onOpenSettings = {}) }
        }
    }

    @Test
    fun showsEmptyStateWhenThereAreNoBooks() {
        setContent(LibraryUiState(isLoading = false))

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.library_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun showsBooksOnShelvesWithProgress() {
        val book = Book(
            id = 1,
            title = "Cosmos",
            author = "Carl Sagan",
            currentPage = 120,
            totalPages = 400,
            status = BookStatus.READING,
        )

        setContent(LibraryUiState(books = listOf(book), isLoading = false))

        // Covers expose title, author and progress to screen readers in one description.
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.book_cover_description_progress, "Cosmos", "Carl Sagan", 30),
        ).assertIsDisplayed()
    }

    @Test
    fun clickingACoverOpensTheBook() {
        var openedId: Long? = null
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(Book(id = 7, title = "Dune", author = "Frank Herbert")), isLoading = false),
                    onBookClick = { openedId = it },
                    onAddBook = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.book_cover_description, "Dune", "Frank Herbert"),
        ).performClick()

        assertEquals(7L, openedId)
    }

    @Test
    fun addBookButtonIsShownWithBooks() {
        setContent(LibraryUiState(books = listOf(Book(id = 1, title = "Dune", author = "Frank Herbert")), isLoading = false))

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.library_add_book)).assertIsDisplayed()
    }
}
