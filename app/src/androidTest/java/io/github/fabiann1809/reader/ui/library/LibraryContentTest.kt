package io.github.fabiann1809.reader.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(uiState: LibraryUiState) {
        composeRule.setContent {
            ReaderTheme { LibraryContent(uiState = uiState, onBookClick = {}, onAddBook = {}) }
        }
    }

    @Test
    fun showsEmptyStateWhenThereAreNoBooks() {
        setContent(LibraryUiState(isLoading = false, libraryIsEmpty = true))

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

    private fun string(id: Int) = composeRule.activity.getString(id)

    @Test
    fun titleShowsTheCollectionAndOpensThePicker() {
        setContent(LibraryUiState(isLoading = false, filter = LibraryFilter.Smart(SmartCollection.FAVORITES)))

        composeRule.onNodeWithText(string(R.string.collection_favorites)).performClick()

        composeRule.onNodeWithText(string(R.string.collections_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.collection_new)).assertIsDisplayed()
    }

    @Test
    fun emptyCollectionOffersToShowAllBooks() {
        var selected: LibraryFilter? = null
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(
                        isLoading = false,
                        filter = LibraryFilter.Custom(1),
                        currentCollection = Collection(id = 1, name = "Trabajo"),
                    ),
                    onBookClick = {},
                    onAddBook = {},
                    onSelectFilter = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("Trabajo").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.collection_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.collection_show_all)).performClick()
        assertEquals(LibraryFilter.Default, selected)
    }

    @Test
    fun customCollectionMenuRenamesAndDeletes() {
        var renamedTo: String? = null
        var deleted = false
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(
                        isLoading = false,
                        filter = LibraryFilter.Custom(1),
                        currentCollection = Collection(id = 1, name = "Trabajo"),
                    ),
                    onBookClick = {},
                    onAddBook = {},
                    onRenameCollection = { renamedTo = it },
                    onDeleteCollection = { deleted = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription(string(R.string.more_options)).performClick()
        composeRule.onNodeWithText(string(R.string.collection_rename)).performClick()
        composeRule.onNodeWithText(string(R.string.collection_rename_confirm)).performClick()
        assertEquals("Trabajo", renamedTo)

        composeRule.onNodeWithContentDescription(string(R.string.more_options)).performClick()
        composeRule.onNodeWithText(string(R.string.collection_delete)).performClick()
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertTrue(deleted)
    }

    @Test
    fun searchSendsTheTextAndClosingClearsIt() {
        val queries = mutableListOf<String>()
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(Book(id = 1, title = "Dune", author = "F. Herbert")), isLoading = false),
                    onBookClick = {},
                    onAddBook = {},
                    onSearch = { queries += it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(string(R.string.library_search)).performClick()
        composeRule.onNodeWithText(string(R.string.library_search_hint)).performTextInput("dune")
        composeRule.onNodeWithContentDescription(string(R.string.library_search_close)).performClick()

        assertEquals(listOf("dune", ""), queries)
    }

    @Test
    fun searchWithoutMatchesOffersToClearIt() {
        var cleared = false
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(isLoading = false, query = "zzz"),
                    onBookClick = {},
                    onAddBook = {},
                    onSearch = { if (it.isEmpty()) cleared = true },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.library_search_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.library_search_clear)).performClick()
        assertTrue(cleared)
    }
}
