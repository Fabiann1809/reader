package io.github.fabiann1809.reader.ui.bookdetail

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightColor
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeCollectionRepository
import io.github.fabiann1809.reader.testing.FakeHighlightRepository
import io.github.fabiann1809.reader.testing.FakeNoteRepository
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

class BookDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val book = Book(id = 1, title = "Dune", author = "Frank Herbert")
    private val bookRepository = FakeBookRepository(listOf(book))
    private val noteRepository = FakeNoteRepository()
    private val collectionRepository = FakeCollectionRepository(bookRepository)
    private val highlightRepository = FakeHighlightRepository()
    // Lazy so it is built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private val viewModel by lazy {
        BookDetailViewModel(
            bookId = 1,
            bookRepository = bookRepository,
            noteRepository = noteRepository,
            collectionRepository = collectionRepository,
            highlightRepository = highlightRepository,
            organizer = BookOrganizer(bookRepository, collectionRepository),
        )
    }

    // stateIn(WhileSubscribed) only runs the queries while someone collects the state.
    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun initialStateIsLoading() {
        assertEquals(BookDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun emitsBookWithEmptyNotes() = runTest {
        collectUiState()

        assertEquals(BookDetailUiState.Success(book, emptyList()), viewModel.uiState.value)
    }

    @Test
    fun emitsOnlyThisBooksNotesNewestFirst() = runTest {
        noteRepository.addNote(Note(bookId = 1, content = "Old", createdAt = 1_000))
        noteRepository.addNote(Note(bookId = 1, content = "New", createdAt = 2_000))
        noteRepository.addNote(Note(bookId = 2, content = "Other book"))
        collectUiState()

        val state = viewModel.uiState.value as BookDetailUiState.Success
        assertEquals(listOf("New", "Old"), state.notes.map { it.content })
    }

    @Test
    fun emitsThisBooksHighlightsInReadingOrder() = runTest {
        highlightRepository.addHighlight(Highlight(bookId = 1, location = "{}", text = "Final", progression = 0.9))
        highlightRepository.addHighlight(Highlight(bookId = 1, location = "{}", text = "Inicio", progression = 0.1, color = HighlightColor.PINK))
        highlightRepository.addHighlight(Highlight(bookId = 2, location = "{}", text = "Otro libro"))
        collectUiState()

        val highlights = (viewModel.uiState.value as BookDetailUiState.Success).highlights

        assertEquals(listOf("Inicio", "Final"), highlights.map { it.text })
        assertEquals(HighlightColor.PINK, highlights.first().color)
    }

    @Test
    fun emitsNotFoundWhenBookIsMissingOrDeleted() = runTest {
        collectUiState()

        bookRepository.deleteBook(book)

        assertEquals(BookDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun updateProgressPersistsPageAndStatus() = runTest {
        viewModel.updateProgress(currentPage = 150, status = BookStatus.READING)

        val stored = bookRepository.getBook(1)
        assertEquals(150, stored?.currentPage)
        assertEquals(BookStatus.READING, stored?.status)
        assertEquals("Dune", stored?.title)
    }

    @Test
    fun deleteBookRemovesItAndSignalsDeletion() = runTest {
        viewModel.deleteBook()

        assertNull(bookRepository.getBook(1))
        assertTrue(viewModel.isDeleted.value)
    }

    @Test
    fun setFavoritePutsTheBookInMyFavorites() = runTest {
        viewModel.setFavorite(true)

        val favorites = collectionRepository.observeBooks(LibraryFilter.Smart(SmartCollection.FAVORITES)).first()
        assertEquals(listOf("Dune"), favorites.map { it.title })

        viewModel.setFavorite(false)

        assertFalse(bookRepository.getBook(1)!!.isFavorite)
    }

    @Test
    fun setInCollectionAddsAndRemovesTheBook() = runTest {
        val id = collectionRepository.createCollection("Ciencia ficción")
        collectionRepository.createCollection("Otra")
        collectUiState()

        viewModel.setInCollection(id, isIncluded = true)

        val state = viewModel.uiState.value as BookDetailUiState.Success
        assertEquals(listOf("Ciencia ficción", "Otra"), state.collections.map { it.name })
        assertEquals(setOf(id), state.collectionIds)

        viewModel.setInCollection(id, isIncluded = false)

        assertEquals(emptySet<Long>(), (viewModel.uiState.value as BookDetailUiState.Success).collectionIds)
    }

    @Test
    fun createCollectionWithBookAddsTheBookToIt() = runTest {
        collectUiState()

        viewModel.createCollectionWithBook("  Para el trabajo ")

        val state = viewModel.uiState.value as BookDetailUiState.Success
        assertEquals(listOf("Para el trabajo"), state.collections.map { it.name })
        assertEquals(setOf(state.collections.single().id), state.collectionIds)
    }

    @Test
    fun createCollectionWithBlankNameDoesNothing() = runTest {
        viewModel.createCollectionWithBook("   ")

        assertTrue(collectionRepository.observeCollections().first().isEmpty())
    }
}
