package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.readium.r2.shared.publication.Publication

class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(listOf(Book(id = 1, title = "El principito", author = "Saint-Exupéry")))

    /** Opens every book unless told what problem to report. */
    private class FakeSession(private val problem: OpenProblem? = null) : ReaderSession {
        val opened = mutableListOf<Long>()

        override suspend fun open(book: Book): OpenProblem? = problem.also { if (it == null) opened += book.id }

        override fun publication(bookId: Long): Publication? = null

        override fun close(bookId: Long) = Unit
    }

    @Test
    fun anOpenedBookIsReady() {
        val session = FakeSession()

        val viewModel = ReaderViewModel(bookId = 1, bookRepository = books, session = session)

        assertEquals(ReaderUiState.Ready(bookId = 1), viewModel.uiState.value)
        assertEquals(listOf(1L), session.opened)
    }

    @Test
    fun aBookThatCannotOpenSaysWhy() {
        val viewModel = ReaderViewModel(bookId = 1, bookRepository = books, session = FakeSession(OpenProblem.NOT_SUPPORTED_YET))

        assertEquals(ReaderUiState.CannotOpen(OpenProblem.NOT_SUPPORTED_YET), viewModel.uiState.value)
    }

    @Test
    fun aDeletedBookHasNoFile() {
        val viewModel = ReaderViewModel(bookId = 99, bookRepository = books, session = FakeSession())

        assertEquals(ReaderUiState.CannotOpen(OpenProblem.NO_FILE), viewModel.uiState.value)
    }
}
