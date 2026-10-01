package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookSort
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.book.importing.BookImporter
import io.github.fabiann1809.reader.data.book.importing.FailedImport
import io.github.fabiann1809.reader.data.book.importing.ImportQueue
import io.github.fabiann1809.reader.data.book.importing.ImportResult
import io.github.fabiann1809.reader.data.book.importing.ImportStatus
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeCollectionRepository
import io.github.fabiann1809.reader.testing.FakeFailedImportStore
import io.github.fabiann1809.reader.testing.FakeFileAccess
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
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

    // Pretends to import: adds a digital book unless told to fail.
    private var importOutcome: ImportResult? = null
    private val importer = BookImporter { uri ->
        importOutcome ?: ImportResult.Imported(books.addBook(Book(title = uri.substringAfterLast('/'), author = "")))
    }

    private fun TestScope.viewModel(bookRepository: FakeBookRepository = books): LibraryViewModel {
        // Imports run eagerly, as the app's main-thread scope would between two frames.
        val importScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val viewModel = LibraryViewModel(
            bookRepository,
            collections,
            preferences,
            ImportQueue(importer, importScope, FakeFileAccess(), FakeFailedImportStore()),
            BookOrganizer(bookRepository, collections),
        )
        // stateIn(WhileSubscribed) only runs the queries while someone collects.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel
    }

    private fun LibraryViewModel.titles() = uiState.value.books.map { it.title }

    @Test
    fun initialStateIsLoading() {
        assertTrue(LibraryViewModel(
                books,
                collections,
                preferences,
                ImportQueue(importer, TestScope(), FakeFileAccess(), FakeFailedImportStore()),
                BookOrganizer(books, collections),
            ).uiState.value.isLoading)
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

    @Test
    fun arrangementSortsFiltersAndIsRemembered() = runTest {
        val viewModel = viewModel()
        val arrangement = LibraryArrangement(sort = BookSort.TITLE)

        viewModel.setArrangement(arrangement)
        assertEquals(listOf("Cosmos", "Dune"), viewModel.titles())

        viewModel.setArrangement(arrangement.copy(statuses = setOf(BookStatus.READING)))
        assertEquals(listOf("Dune"), viewModel.titles())
        assertEquals(setOf(BookStatus.READING), preferences.libraryArrangement.first().statuses)
        assertTrue(viewModel.uiState.value.arrangement.hasFilters)
    }

    @Test
    fun layoutStartsOnShelvesAndIsRemembered() = runTest {
        val viewModel = viewModel()
        assertEquals(LibraryLayout(view = LibraryView.SHELVES, booksPerRow = 3), viewModel.uiState.value.layout)

        viewModel.setLayout(LibraryLayout(view = LibraryView.GRID, booksPerRow = 4))

        assertEquals(LibraryLayout(view = LibraryView.GRID, booksPerRow = 4), viewModel.uiState.value.layout)
        assertEquals(LibraryLayout(view = LibraryView.GRID, booksPerRow = 4), preferences.libraryLayout.first())
    }

    @Test
    fun markAsReadFinishesTheBook() = runTest {
        val viewModel = viewModel()

        viewModel.markAsRead(1)

        assertEquals(BookStatus.FINISHED, books.getBook(1)?.status)
    }

    @Test
    fun deleteBookRemovesItFromTheShelf() = runTest {
        val viewModel = viewModel()

        viewModel.deleteBook(1)

        assertNull(books.getBook(1))
        assertEquals(listOf("Cosmos"), viewModel.titles())
    }

    @Test
    fun collectionSheetFollowsTheBooksCollectionsWithoutChangingTheShelf() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.bookCollections.collect() }

        viewModel.showCollectionsOf(1)
        viewModel.createCollectionWithBook(1, "Ciencia ficción")
        viewModel.setFavorite(1, true)

        val sheet = viewModel.bookCollections.value
        assertEquals("Dune", sheet?.book?.title)
        assertTrue(sheet!!.book.isFavorite)
        assertEquals(1, sheet.collectionIds.size)
        assertEquals(LibraryFilter.Default, viewModel.uiState.value.filter)

        viewModel.setInCollection(1, sheet.collectionIds.single(), isIncluded = false)
        assertTrue(viewModel.bookCollections.value!!.collectionIds.isEmpty())

        viewModel.hideCollections()
        assertNull(viewModel.bookCollections.value)
    }

    @Test
    fun togglingSelectsAndTheLastUncheckEndsSelection() = runTest {
        val viewModel = viewModel()

        viewModel.toggleSelection(1)
        viewModel.toggleSelection(2)
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedIds)
        assertTrue(viewModel.uiState.value.isSelecting)

        viewModel.toggleSelection(1)
        viewModel.toggleSelection(2)
        assertFalse(viewModel.uiState.value.isSelecting)
    }

    @Test
    fun booksHiddenBySearchAreNotActedOn() = runTest {
        val viewModel = viewModel()
        viewModel.toggleSelection(1)
        viewModel.toggleSelection(2)

        viewModel.search("dune")
        assertEquals(setOf(1L), viewModel.uiState.value.selectedIds)

        viewModel.deleteSelected()
        viewModel.search("")

        assertEquals(listOf("Cosmos"), viewModel.titles())
        assertFalse(viewModel.uiState.value.isSelecting)
    }

    @Test
    fun selectedBooksGoToACollectionOrFavoritesAndSelectionEnds() = runTest {
        val viewModel = viewModel()
        val collectionId = collections.createCollection("Ciencia")

        viewModel.toggleSelection(1)
        viewModel.toggleSelection(2)
        viewModel.addSelectedToCollection(collectionId)
        assertFalse(viewModel.uiState.value.isSelecting)

        viewModel.toggleSelection(1)
        viewModel.addSelectedToFavorites()

        viewModel.selectFilter(LibraryFilter.Custom(collectionId))
        assertEquals(listOf("Cosmos", "Dune"), viewModel.titles())
        viewModel.selectFilter(LibraryFilter.Smart(SmartCollection.FAVORITES))
        assertEquals(listOf("Cosmos", "Dune"), viewModel.titles())
    }

    @Test
    fun createCollectionWithSelectedHoldsEverySelectedBook() = runTest {
        val viewModel = viewModel()
        viewModel.toggleSelection(1)
        viewModel.toggleSelection(2)

        viewModel.createCollectionWithSelected("Clásicos")

        val created = viewModel.uiState.value.collections.single()
        assertEquals("Clásicos", created.name)
        assertEquals(setOf(1L, 2L), collections.observeBooks(LibraryFilter.Custom(created.id)).first().map { it.id }.toSet())
        assertFalse(viewModel.uiState.value.isSelecting)
    }

    @Test
    fun importedBooksAppearOnTheShelf() = runTest {
        val viewModel = viewModel()

        viewModel.importBooks(listOf("content://picker/Neuromante", "content://picker/Solaris"))

        assertEquals(ImportStatus.Idle, viewModel.importStatus.value)
        assertTrue(viewModel.titles().containsAll(listOf("Neuromante", "Solaris")))
    }

    @Test
    fun failedImportsAreShownUntilDiscarded() = runTest {
        val viewModel = viewModel()

        importOutcome = ImportResult.Unsupported(fileName = "foto.jpg")
        viewModel.importBooks(listOf("content://picker/foto.jpg"))
        assertEquals(
            ImportStatus.Failed(listOf(FailedImport("content://picker/foto.jpg", "foto.jpg", isUnsupported = true)), 0),
            viewModel.importStatus.value,
        )

        // Retrying after fixing the problem imports it.
        importOutcome = null
        viewModel.retryFailedImports()
        assertEquals(ImportStatus.Idle, viewModel.importStatus.value)
        assertTrue("foto.jpg" in viewModel.titles())

        importOutcome = ImportResult.Failed(fileName = null)
        viewModel.importBooks(listOf("content://picker/roto.epub"))
        viewModel.dismissFailedImports()
        assertEquals(ImportStatus.Idle, viewModel.importStatus.value)
    }

    @Test
    fun continueOffersTheLastReadDigitalBookWhateverTheShelf() = runTest {
        val viewModel = viewModel()
        assertNull(viewModel.uiState.value.bookToContinue)

        books.addBook(
            Book(id = 3, title = "El principito", author = "", kind = BookKind.DIGITAL, lastOpenedAt = 100, createdAt = 3),
        )
        viewModel.selectFilter(LibraryFilter.Smart(SmartCollection.FAVORITES))

        assertEquals("El principito", viewModel.uiState.value.bookToContinue?.title)
    }
}
