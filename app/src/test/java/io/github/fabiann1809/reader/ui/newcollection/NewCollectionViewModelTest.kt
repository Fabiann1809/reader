package io.github.fabiann1809.reader.ui.newcollection

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeCollectionRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NewCollectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(
        listOf(
            Book(id = 1, title = "Dune", author = "F. Herbert", createdAt = 1),
            Book(id = 2, title = "Cosmos", author = "C. Sagan", createdAt = 2),
        ),
    )
    private val collections = FakeCollectionRepository(books)
    private val preferences = FakeAppPreferences()

    private fun viewModel() = NewCollectionViewModel(books, collections, BookOrganizer(books, collections), preferences)

    @Test
    fun listsTheLibraryNewestFirst() = runTest {
        val state = viewModel().uiState.first { it.books.isNotEmpty() }

        assertEquals(listOf("Cosmos", "Dune"), state.books.map { it.title })
    }

    @Test
    fun needsANameAndAtLeastOneBookToSave() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.books.isNotEmpty() }
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onNameChange("Verano")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.toggleBook(1)
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.toggleBook(1)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun savingCreatesTheCollectionWithItsBooksAndColorAndShowsIt() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.first { it.books.isNotEmpty() }
        viewModel.onNameChange("  Verano ")
        viewModel.onColorChange(2)
        viewModel.toggleBook(2)

        viewModel.save()

        val created = collections.observeCollections().first().single()
        assertEquals("Verano", created.name)
        assertEquals(2, created.colorIndex)
        assertEquals(listOf(2L), collections.observeBooks(LibraryFilter.Custom(created.id)).first().map { it.id })
        assertEquals(LibraryFilter.Custom(created.id), preferences.libraryFilter.first())
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun startsOnTheColorTheNextCollectionWouldGet() = runTest {
        collections.createCollection("Una")
        collections.createCollection("Otra")

        val viewModel = viewModel()

        assertEquals(2, viewModel.uiState.first { it.books.isNotEmpty() }.colorIndex)
    }
}
