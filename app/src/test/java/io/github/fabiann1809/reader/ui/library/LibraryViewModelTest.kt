package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeCollectionRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(
        listOf(
            Book(id = 1, title = "Dune", author = "F. Herbert", status = BookStatus.READING, createdAt = 1),
            Book(id = 2, title = "Cosmos", author = "C. Sagan", isFavorite = true, createdAt = 2),
        ),
    )
    private val collections = FakeCollectionRepository(books)
    private val preferences = FakeAppPreferences()

    private fun TestScope.viewModel(bookRepository: FakeBookRepository = books): LibraryViewModel {
        val viewModel = LibraryViewModel(bookRepository, collections, preferences)
        // stateIn(WhileSubscribed) only runs the queries while someone collects.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel
    }

    private fun LibraryViewModel.titles() = uiState.value.books.map { it.title }

    @Test
    fun initialStateIsLoading() {
        assertTrue(LibraryViewModel(books, collections, preferences).uiState.value.isLoading)
    }

    @Test
    fun startsOnAllBooks() = runTest {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(LibraryFilter.Default, viewModel.uiState.value.filter)
        assertEquals(listOf("Cosmos", "Dune"), viewModel.titles())
    }

    @Test
    fun selectingASmartCollectionFiltersAndIsRemembered() = runTest {
        val viewModel = viewModel()

        viewModel.selectFilter(LibraryFilter.Smart(SmartCollection.FAVORITES))

        assertEquals(listOf("Cosmos"), viewModel.titles())
        assertEquals(LibraryFilter.Smart(SmartCollection.FAVORITES), preferences.libraryFilter.first())
    }

    @Test
    fun creatingACollectionSelectsItAndItStartsEmpty() = runTest {
        val viewModel = viewModel()

        viewModel.createCollection("Trabajo")

        val state = viewModel.uiState.value
        assertEquals("Trabajo", state.currentCollection?.name)
        assertEquals(listOf("Trabajo"), state.collections.map { it.name })
        assertTrue(state.books.isEmpty())
        // The library itself still has books: this is the "empty collection" state.
        assertFalse(state.libraryIsEmpty)
    }

    @Test
    fun customCollectionShowsOnlyItsBooks() = runTest {
        val viewModel = viewModel()
        val id = collections.createCollection("Trabajo")
        collections.addBook(bookId = 1, collectionId = id)

        viewModel.selectFilter(LibraryFilter.Custom(id))

        assertEquals(listOf("Dune"), viewModel.titles())
    }

    @Test
    fun renamingChangesTheTitle() = runTest {
        val viewModel = viewModel()
        viewModel.createCollection("Trabajo")

        viewModel.renameCurrentCollection("Oficina")

        assertEquals("Oficina", viewModel.uiState.value.currentCollection?.name)
    }

    @Test
    fun deletingTheCollectionGoesBackToAllAndKeepsBooks() = runTest {
        val viewModel = viewModel()
        viewModel.createCollection("Trabajo")

        viewModel.deleteCurrentCollection()

        val state = viewModel.uiState.value
        assertEquals(LibraryFilter.Default, state.filter)
        assertNull(state.currentCollection)
        assertTrue(state.collections.isEmpty())
        assertEquals(listOf("Cosmos", "Dune"), viewModel.titles())
    }

    @Test
    fun aRememberedCollectionThatNoLongerExistsFallsBackToAll() = runTest {
        preferences.setLibraryFilter(LibraryFilter.Custom(collectionId = 99))

        val viewModel = viewModel()

        assertEquals(LibraryFilter.Default, viewModel.uiState.value.filter)
        assertEquals(listOf("Cosmos", "Dune"), viewModel.titles())
    }

    @Test
    fun emptyLibraryIsReported() = runTest {
        val viewModel = viewModel(FakeBookRepository())

        assertTrue(viewModel.uiState.value.libraryIsEmpty)
    }

    @Test
    fun reactsToNewBooks() = runTest {
        val viewModel = viewModel()

        books.addBook(Book(title = "Nuevo", author = "A", createdAt = Long.MAX_VALUE))

        assertEquals(listOf("Nuevo", "Cosmos", "Dune"), viewModel.titles())
    }

    @Test
    fun searchFiltersByTitleOrAuthorWithinTheCollection() = runTest {
        val viewModel = viewModel()

        viewModel.search("sagan")
        assertEquals(listOf("Cosmos"), viewModel.titles())

        viewModel.search("DUN")
        assertEquals(listOf("Dune"), viewModel.titles())

        viewModel.selectFilter(LibraryFilter.Smart(SmartCollection.FAVORITES))
        assertEquals(emptyList<String>(), viewModel.titles())
        assertFalse(viewModel.uiState.value.libraryIsEmpty)

        viewModel.search("")
        assertEquals(listOf("Cosmos"), viewModel.titles())
    }
}
