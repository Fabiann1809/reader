package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.testing.FakeHighlightRepository
import io.github.fabiann1809.reader.testing.FakeNoteRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReaderNotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun listsOnlyTheOpenBooksNotesNewestFirstAndItsHighlights() = runTest {
        val notes = FakeNoteRepository(
            listOf(
                Note(id = 1, bookId = 1, content = "Old", createdAt = 1_000),
                Note(id = 2, bookId = 1, content = "New", createdAt = 2_000),
                Note(id = 3, bookId = 2, content = "Other book", createdAt = 3_000),
            ),
        )
        val highlights = FakeHighlightRepository(
            listOf(
                Highlight(id = 1, bookId = 1, location = "{}", text = "Mine"),
                Highlight(id = 2, bookId = 2, location = "{}", text = "Other"),
            ),
        )
        val viewModel = ReaderNotesViewModel(bookId = 1, noteRepository = notes, highlightRepository = highlights)

        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(listOf("New", "Old"), state.notes.map { it.content })
        assertEquals(listOf("Mine"), state.highlights.map { it.text })
    }

    @Test
    fun isEmptyWhenTheBookHasNoNotesNorHighlights() = runTest {
        val viewModel = ReaderNotesViewModel(1, FakeNoteRepository(), FakeHighlightRepository())

        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.notes.isEmpty() && state.highlights.isEmpty())
    }
}
