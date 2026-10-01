package io.github.fabiann1809.reader.ui.notes

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeNoteRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AllNotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = listOf(Book(id = 1, title = "Cosmos", author = "Carl Sagan"), Book(id = 2, title = "Dune", author = "F. Herbert"))

    @Test
    fun showsNotesOfEveryBookNewestFirstWithTheirBookTitle() = runTest {
        val notes = FakeNoteRepository(
            listOf(
                Note(id = 1, bookId = 1, content = "Old", createdAt = 1_000),
                Note(id = 2, bookId = 2, content = "New", createdAt = 2_000),
            ),
        )
        val viewModel = AllNotesViewModel(FakeBookRepository(books), notes)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf("New" to "Dune", "Old" to "Cosmos"), state.notes.map { it.note.content to it.bookTitle })
    }

    @Test
    fun isEmptyWhenThereAreNoNotes() = runTest {
        val viewModel = AllNotesViewModel(FakeBookRepository(books), FakeNoteRepository())

        assertTrue(viewModel.uiState.first { !it.isLoading }.notes.isEmpty())
    }
}
