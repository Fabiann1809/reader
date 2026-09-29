package io.github.fabiann1809.reader.ui.bookdetail

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val book = Book(id = 1, title = "Dune", author = "Frank Herbert")

    @Test
    fun initialStateIsLoading() {
        val viewModel = BookDetailViewModel(bookId = 1, bookRepository = FakeBookRepository(listOf(book)))

        assertEquals(BookDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun emitsBookWhenItExists() = runTest {
        val viewModel = BookDetailViewModel(bookId = 1, bookRepository = FakeBookRepository(listOf(book)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(BookDetailUiState.Success(book), viewModel.uiState.value)
    }

    @Test
    fun emitsNotFoundWhenBookIsMissingOrDeleted() = runTest {
        val repository = FakeBookRepository(listOf(book))
        val viewModel = BookDetailViewModel(bookId = 1, bookRepository = repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        repository.deleteBook(book)

        assertEquals(BookDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun updateProgressPersistsPageAndStatus() = runTest {
        val repository = FakeBookRepository(listOf(book))
        val viewModel = BookDetailViewModel(bookId = 1, bookRepository = repository)

        viewModel.updateProgress(currentPage = 150, status = BookStatus.READING)

        val stored = repository.getBook(1)
        assertEquals(150, stored?.currentPage)
        assertEquals(BookStatus.READING, stored?.status)
        assertEquals("Dune", stored?.title)
    }

    @Test
    fun deleteBookRemovesItAndSignalsDeletion() = runTest {
        val repository = FakeBookRepository(listOf(book))
        val viewModel = BookDetailViewModel(bookId = 1, bookRepository = repository)

        viewModel.deleteBook()

        assertNull(repository.getBook(1))
        assertTrue(viewModel.isDeleted.value)
    }
}
