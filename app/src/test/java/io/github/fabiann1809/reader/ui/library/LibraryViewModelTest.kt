package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateIsLoading() {
        val viewModel = LibraryViewModel(FakeBookRepository())

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun emitsEmptyListWhenThereAreNoBooks() = runTest {
        val viewModel = LibraryViewModel(FakeBookRepository())
        // stateIn(WhileSubscribed) only runs the query while someone collects it.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(emptyList<Book>(), state.books)
    }

    @Test
    fun emitsBooksFromRepositoryAndReactsToChanges() = runTest {
        val repository = FakeBookRepository(listOf(Book(id = 1, title = "Dune", author = "Frank Herbert")))
        val viewModel = LibraryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(listOf("Dune"), viewModel.uiState.value.books.map { it.title })

        repository.addBook(Book(title = "Cosmos", author = "Carl Sagan", createdAt = Long.MAX_VALUE))

        assertEquals(listOf("Cosmos", "Dune"), viewModel.uiState.value.books.map { it.title })
    }
}
