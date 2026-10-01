package io.github.fabiann1809.reader.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookSort
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            ReaderTheme { LibraryContent(uiState = uiState, onBookClick = {}, onAddPhysicalBook = {}) }
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
            substring = true,
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
                    onAddPhysicalBook = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.book_cover_description, "Dune", "Frank Herbert"),
            substring = true,
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
                    onAddPhysicalBook = {},
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
                    onAddPhysicalBook = {},
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
                    onAddPhysicalBook = {},
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
                    onAddPhysicalBook = {},
                    onSearch = { if (it.isEmpty()) cleared = true },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.library_search_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.library_search_clear)).performClick()
        assertTrue(cleared)
    }

    @Test
    fun arrangeSheetChangesSortAndFilters() {
        val changes = mutableListOf<LibraryArrangement>()
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(Book(id = 1, title = "Dune", author = "F. Herbert")), isLoading = false),
                    onBookClick = {},
                    onAddPhysicalBook = {},
                    onArrangementChange = { changes += it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(string(R.string.library_arrange)).performClick()
        composeRule.onNodeWithText(string(R.string.sort_title)).performClick()
        composeRule.onNodeWithText(string(R.string.book_status_reading)).performClick()

        assertEquals(
            listOf(
                LibraryArrangement(sort = BookSort.TITLE),
                LibraryArrangement(statuses = setOf(BookStatus.READING)),
            ),
            changes,
        )
    }

    @Test
    fun filtersHidingEverythingOfferToClearThem() {
        var cleared: LibraryArrangement? = null
        val filtered = LibraryArrangement(sort = BookSort.AUTHOR, statuses = setOf(BookStatus.FINISHED))
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(isLoading = false, arrangement = filtered),
                    onBookClick = {},
                    onAddPhysicalBook = {},
                    onArrangementChange = { cleared = it },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.library_filters_empty_message)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.arrange_clear_filters)).performClick()
        assertEquals(LibraryArrangement(sort = BookSort.AUTHOR), cleared)
    }

    @Test
    fun listViewShowsTitleAuthorAndOpensTheBook() {
        var opened: Long? = null
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(
                        books = listOf(Book(id = 4, title = "Dune", author = "Frank Herbert", currentPage = 5)),
                        isLoading = false,
                        layout = LibraryLayout(view = LibraryView.LIST),
                    ),
                    onBookClick = { opened = it },
                    onAddPhysicalBook = {},
                )
            }
        }

        composeRule.onNodeWithText("Frank Herbert").assertIsDisplayed()
        composeRule.onNodeWithText("Dune").performClick()
        assertEquals(4L, opened)
    }

    @Test
    fun gridViewOpensTheBook() {
        var opened: Long? = null
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(
                        books = listOf(Book(id = 4, title = "Dune", author = "Frank Herbert")),
                        isLoading = false,
                        layout = LibraryLayout(view = LibraryView.GRID, booksPerRow = 2),
                    ),
                    onBookClick = { opened = it },
                    onAddPhysicalBook = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.book_cover_description, "Dune", "Frank Herbert"),
            substring = true,
        )
            .performClick()
        assertEquals(4L, opened)
    }

    @Test
    fun sheetChangesViewAndBooksPerRow() {
        val layouts = mutableListOf<LibraryLayout>()
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(Book(id = 1, title = "Dune", author = "F. Herbert")), isLoading = false),
                    onBookClick = {},
                    onAddPhysicalBook = {},
                    onLayoutChange = { layouts += it },
                )
            }
        }

        composeRule.onNodeWithContentDescription(string(R.string.library_arrange)).performClick()
        composeRule.onNodeWithText(string(R.string.view_grid)).performClick()
        composeRule.onNodeWithText("4").performClick()

        assertEquals(listOf(LibraryLayout(view = LibraryView.GRID), LibraryLayout(booksPerRow = 4)), layouts)
    }

    private fun setContentWithMenu(book: Book, actions: LibraryBookActions, onBookClick: (Long) -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(book), isLoading = false),
                    onBookClick = onBookClick,
                    onAddPhysicalBook = {},
                    bookActions = actions,
                )
            }
        }
    }

    private fun longPressCover(book: Book) {
        composeRule.onNodeWithContentDescription(string(R.string.book_cover_description).format(book.title, book.author), substring = true)
            .performTouchInput { longClick() }
    }

    @Test
    fun longPressMenuOpensDetailMarksAsReadAndShowsCollections() {
        val book = Book(id = 9, title = "Dune", author = "Frank Herbert")
        var opened: Long? = null
        var markedAsRead: Long? = null
        var collectionsOf: Long? = null
        setContentWithMenu(
            book,
            LibraryBookActions(onMarkAsRead = { markedAsRead = it }, onShowCollections = { collectionsOf = it }),
            onBookClick = { opened = it },
        )

        // Paper books cannot be opened in the app, so "Abrir" is not offered.
        longPressCover(book)
        composeRule.onNodeWithText(string(R.string.book_menu_open)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.book_menu_detail)).performClick()
        longPressCover(book)
        composeRule.onNodeWithText(string(R.string.book_menu_mark_read)).performClick()
        longPressCover(book)
        composeRule.onNodeWithText(string(R.string.book_menu_collection)).performClick()

        assertEquals(9L, opened)
        assertEquals(9L, markedAsRead)
        assertEquals(9L, collectionsOf)
    }

    @Test
    fun deletingFromTheMenuAsksForConfirmation() {
        val book = Book(id = 9, title = "Dune", author = "Frank Herbert")
        var deleted: Long? = null
        setContentWithMenu(book, LibraryBookActions(onDelete = { deleted = it }))

        longPressCover(book)
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertNull(deleted)

        composeRule.onNodeWithText(string(R.string.delete_book_message).format("Dune")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertEquals(9L, deleted)
    }

    @Test
    fun newPhysicalBookShowsTheNewBadge() {
        setContent(LibraryUiState(books = listOf(Book(id = 1, title = "Dune", author = "Frank Herbert")), isLoading = false))

        composeRule.onNodeWithContentDescription(string(R.string.book_badge_new), substring = true).assertExists()
    }

    @Test
    fun selectionModeTogglesOnTapAndConfirmsDeletion() {
        val dune = Book(id = 1, title = "Dune", author = "Frank Herbert")
        val cosmos = Book(id = 2, title = "Cosmos", author = "Carl Sagan")
        val toggled = mutableListOf<Long>()
        var deleted = false
        var opened = false
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(dune, cosmos), isLoading = false, selectedIds = setOf(1, 2)),
                    onBookClick = { opened = true },
                    onAddPhysicalBook = {},
                    selectionActions = LibrarySelectionActions(onToggle = { toggled += it }, onDelete = { deleted = true }),
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.resources.getQuantityString(R.plurals.selection_count, 2, 2))
            .assertIsDisplayed()
        // A tap checks or unchecks instead of opening the book.
        composeRule.onNodeWithContentDescription(string(R.string.book_cover_description).format("Cosmos", "Carl Sagan"), substring = true)
            .performClick()
        assertEquals(listOf(2L), toggled)
        assertFalse(opened)

        composeRule.onNodeWithContentDescription(string(R.string.action_delete)).performClick()
        assertFalse(deleted)
        composeRule.onNodeWithText(composeRule.activity.resources.getQuantityString(R.plurals.delete_books_message, 2, 2))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_delete)).performClick()
        assertTrue(deleted)
    }

    @Test
    fun selectFromTheCoverMenuStartsSelection() {
        val book = Book(id = 5, title = "Dune", author = "Frank Herbert")
        var selected: Long? = null
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(book), isLoading = false),
                    onBookClick = {},
                    onAddPhysicalBook = {},
                    selectionActions = LibrarySelectionActions(onToggle = { selected = it }),
                )
            }
        }

        longPressCover(book)
        composeRule.onNodeWithText(string(R.string.book_menu_select)).performClick()

        assertEquals(5L, selected)
    }

    @Test
    fun addBookOffersAFileOrAPhysicalBook() {
        var importedFile = false
        var addedPhysical = false
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(isLoading = false, libraryIsEmpty = true),
                    onBookClick = {},
                    onAddPhysicalBook = { addedPhysical = true },
                    onImportFile = { importedFile = true },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.library_add_book)).performClick()
        composeRule.onNodeWithText(string(R.string.add_book_from_file)).performClick()
        assertTrue(importedFile)

        composeRule.onNodeWithText(string(R.string.library_add_book)).performClick()
        composeRule.onNodeWithText(string(R.string.add_book_physical)).performClick()
        assertTrue(addedPhysical)
    }

    @Test
    fun importProgressShowsTheCount() {
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(isLoading = false, libraryIsEmpty = true),
                    onBookClick = {},
                    onAddPhysicalBook = {},
                    importStatus = ImportStatus.Importing(done = 2, total = 7),
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.resources.getQuantityString(R.plurals.import_progress, 7, 7, 3))
            .assertIsDisplayed()
    }

    @Test
    fun importErrorCardListsTheFilesAndOffersRetryAndDiscard() {
        var retried = false
        var dismissed = false
        val failures = listOf(
            FailedImport("content://1", "apuntes-cap3.rar", isUnsupported = true),
            FailedImport("content://2", "manual.djvu", isUnsupported = true),
        )
        composeRule.setContent {
            ReaderTheme {
                LibraryContent(
                    uiState = LibraryUiState(books = listOf(Book(id = 1, title = "Dune", author = "F. Herbert")), isLoading = false),
                    onBookClick = {},
                    onAddPhysicalBook = {},
                    importStatus = ImportStatus.Failed(failures, importedCount = 5),
                    onRetryImports = { retried = true },
                    onDismissImportErrors = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.resources.getQuantityString(R.plurals.import_error_title, 2, 2))
            .assertIsDisplayed()
        composeRule.onNodeWithText("apuntes-cap3.rar · manual.djvu").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.import_error_rest_imported), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.import_error_retry)).performClick()
        composeRule.onNodeWithText(string(R.string.import_error_dismiss)).performClick()
        assertTrue(retried)
        assertTrue(dismissed)
    }
}
