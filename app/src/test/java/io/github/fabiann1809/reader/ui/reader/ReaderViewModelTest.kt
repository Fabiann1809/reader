package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.data.reader.ReadingLocation
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(
        listOf(
            Book(id = 1, title = "El principito", author = "Saint-Exupéry"),
            Book(id = 2, title = "Dune", author = "Frank Herbert", status = BookStatus.FINISHED),
        ),
    )

    /** Opens every book unless told what problem to report; [locations] is driven by the test. */
    private class FakeSession(private val problem: OpenProblem? = null) : ReaderSession {
        val opened = mutableListOf<Long>()
        override val locations = MutableSharedFlow<ReadingLocation>(extraBufferCapacity = 8)

        override suspend fun open(book: Book): OpenProblem? = problem.also { if (it == null) opened += book.id }

        override fun publication(bookId: Long): Publication? = null

        override fun initialLocator(bookId: Long): Locator? = null

        override fun reportLocation(bookId: Long, locator: Locator) = Unit

        override fun close(bookId: Long) = Unit
    }

    private fun viewModel(bookId: Long, session: ReaderSession) =
        ReaderViewModel(bookId = bookId, bookRepository = books, session = session, now = { 5_000L })

    @Test
    fun anOpenedBookIsReady() {
        val session = FakeSession()

        val viewModel = viewModel(bookId = 1, session = session)

        assertEquals(ReaderUiState.Ready(bookId = 1), viewModel.uiState.value)
        assertEquals(listOf(1L), session.opened)
    }

    @Test
    fun aBookThatCannotOpenSaysWhy() {
        val viewModel = viewModel(bookId = 1, session = FakeSession(OpenProblem.NOT_SUPPORTED_YET))

        assertEquals(ReaderUiState.CannotOpen(OpenProblem.NOT_SUPPORTED_YET), viewModel.uiState.value)
    }

    @Test
    fun aDeletedBookHasNoFile() {
        val viewModel = viewModel(bookId = 99, session = FakeSession())

        assertEquals(ReaderUiState.CannotOpen(OpenProblem.NO_FILE), viewModel.uiState.value)
    }

    @Test
    fun openingRecordsWhenAndStartsReading() = runTest {
        viewModel(bookId = 1, session = FakeSession())
        viewModel(bookId = 2, session = FakeSession())

        assertEquals(5_000L, books.getBook(1)?.lastOpenedAt)
        assertEquals(BookStatus.READING, books.getBook(1)?.status)
        // A finished book being reread stays finished.
        assertEquals(BookStatus.FINISHED, books.getBook(2)?.status)
    }

    @Test
    fun aBookThatCannotOpenIsNotMarkedOpened() = runTest {
        viewModel(bookId = 1, session = FakeSession(OpenProblem.UNREADABLE))

        assertNull(books.getBook(1)?.lastOpenedAt)
    }

    @Test
    fun pageTurnsOfThisBookAreSaved() = runTest {
        val session = FakeSession()
        viewModel(bookId = 1, session = session)

        session.locations.emit(ReadingLocation(bookId = 2, json = "{\"other\":true}"))
        session.locations.emit(ReadingLocation(bookId = 1, json = "{\"page\":3}"))

        assertEquals("{\"page\":3}", books.getBook(1)?.readingLocation)
        assertNull(books.getBook(2)?.readingLocation)
    }
}
