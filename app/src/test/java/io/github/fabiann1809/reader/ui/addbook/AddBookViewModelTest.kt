package io.github.fabiann1809.reader.ui.addbook

import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeCoverStore
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.data.book.lookup.BookInfo
import io.github.fabiann1809.reader.data.book.lookup.IsbnLookup
import io.github.fabiann1809.reader.data.book.lookup.IsbnResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddBookViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()
    private val coverStore = FakeCoverStore()
    private var isbnAnswer: IsbnResult = IsbnResult.NotFound
    private val isbnLookup = IsbnLookup { isbnAnswer }
    // Lazy so it is built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private val viewModel by lazy { AddBookViewModel(repository, coverStore, isbnLookup) }

    @Test
    fun cannotSaveUntilTitleAndAuthorAreFilled() {
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onTitleChange("Cosmos")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onAuthorChange("Carl Sagan")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun blankTitleIsNotValid() {
        viewModel.onTitleChange("   ")
        viewModel.onAuthorChange("Carl Sagan")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun totalPagesAcceptsOnlyDigits() {
        viewModel.onTotalPagesChange("12a")
        assertEquals("", viewModel.uiState.value.totalPages)

        viewModel.onTotalPagesChange("350")
        assertEquals("350", viewModel.uiState.value.totalPages)
    }

    @Test
    fun zeroPagesIsInvalid() {
        viewModel.onTitleChange("Cosmos")
        viewModel.onAuthorChange("Carl Sagan")
        viewModel.onTotalPagesChange("0")

        assertFalse(viewModel.uiState.value.isTotalPagesValid)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun saveStoresTrimmedBookAndMarksSaved() {
        viewModel.onTitleChange("  Cosmos ")
        viewModel.onAuthorChange(" Carl Sagan")
        viewModel.onTotalPagesChange("400")

        viewModel.save()

        val saved = repository.currentBooks.single()
        assertEquals("Cosmos", saved.title)
        assertEquals("Carl Sagan", saved.author)
        assertEquals(400, saved.totalPages)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun emptyTotalPagesIsStoredAsNull() {
        viewModel.onTitleChange("Dune")
        viewModel.onAuthorChange("Frank Herbert")

        viewModel.save()

        assertNull(repository.currentBooks.single().totalPages)
    }

    @Test
    fun theChosenCoverIsSavedWithTheBook() = runTest {
        viewModel.onTitleChange("Cosmos")
        viewModel.onAuthorChange("Carl Sagan")
        viewModel.onCoverPicked("content://photos/7")
        viewModel.save()

        assertEquals("covers/7.jpg", repository.currentBooks.single().coverPath)
    }

    @Test
    fun theCoverIsOptional() = runTest {
        viewModel.onTitleChange("Cosmos")
        viewModel.onAuthorChange("Carl Sagan")
        viewModel.save()

        assertNull(repository.currentBooks.single().coverPath)
    }

    @Test
    fun aReplacedOrRemovedCoverIsDeleted() = runTest {
        viewModel.onCoverPicked("content://photos/1")
        viewModel.onCoverPicked("content://photos/2")
        assertEquals(setOf("covers/2.jpg"), coverStore.stored)

        viewModel.onCoverRemoved()

        assertTrue(coverStore.stored.isEmpty())
        assertNull(viewModel.uiState.value.coverPath)
    }

    @Test
    fun anUnreadablePictureChangesNothing() = runTest {
        coverStore.fails = true

        viewModel.onCoverPicked("content://photos/1")

        assertNull(viewModel.uiState.value.coverPath)
    }

    @Test
    fun aFoundIsbnFillsTitleAuthorAndPages() = runTest {
        isbnAnswer = IsbnResult.Found(BookInfo("Cosmos", "Carl Sagan", 365))

        viewModel.onIsbnChange("978-0-306-40615-7")
        viewModel.lookUpIsbn()

        val state = viewModel.uiState.value
        assertEquals("Cosmos", state.title)
        assertEquals("Carl Sagan", state.author)
        assertEquals("365", state.totalPages)
        assertEquals(IsbnStatus.FILLED, state.isbnStatus)
        assertTrue(state.canSave)
    }

    @Test
    fun aMissingAuthorOrPagesKeepWhatWasTyped() = runTest {
        isbnAnswer = IsbnResult.Found(BookInfo("Cosmos", "", null))
        viewModel.onAuthorChange("C. Sagan")
        viewModel.onTotalPagesChange("400")

        viewModel.lookUpIsbn("9780306406157")

        assertEquals("C. Sagan", viewModel.uiState.value.author)
        assertEquals("400", viewModel.uiState.value.totalPages)
    }

    @Test
    fun anInvalidIsbnIsNotSearched() = runTest {
        isbnAnswer = IsbnResult.Found(BookInfo("Cosmos", "Carl Sagan", null))
        viewModel.onIsbnChange("12345")

        viewModel.lookUpIsbn()

        assertEquals(IsbnStatus.NOT_VALID, viewModel.uiState.value.isbnStatus)
        assertEquals("", viewModel.uiState.value.title)
    }

    @Test
    fun anUnknownIsbnOrNoConnectionLeavesTheFormAsIs() = runTest {
        viewModel.onTitleChange("Mi libro")

        isbnAnswer = IsbnResult.NotFound
        viewModel.lookUpIsbn("9780306406157")
        assertEquals(IsbnStatus.NOT_FOUND, viewModel.uiState.value.isbnStatus)

        isbnAnswer = IsbnResult.NoConnection
        viewModel.lookUpIsbn("9780306406157")
        assertEquals(IsbnStatus.NO_CONNECTION, viewModel.uiState.value.isbnStatus)
        assertEquals("Mi libro", viewModel.uiState.value.title)
    }
}
