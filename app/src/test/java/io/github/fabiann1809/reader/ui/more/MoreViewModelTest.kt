package io.github.fabiann1809.reader.ui.more

import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import io.github.fabiann1809.reader.testing.FakeNoteRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun countsNotesAndTheBooksTheyBelongTo() = runTest {
        val notes = FakeNoteRepository(
            listOf(
                Note(id = 1, bookId = 1, content = "A"),
                Note(id = 2, bookId = 1, content = "B"),
                Note(id = 3, bookId = 2, content = "C"),
            ),
        )
        val viewModel = MoreViewModel(notes, FakeApiKeyStore("key"))

        val state = viewModel.uiState.first { it.noteCount > 0 }

        assertEquals(MoreUiState(hasApiKey = true, noteCount = 3, bookCount = 2), state)
    }

    @Test
    fun reportsAMissingApiKey() = runTest {
        val viewModel = MoreViewModel(FakeNoteRepository(), FakeApiKeyStore())

        assertEquals(false, viewModel.uiState.first().hasApiKey)
    }
}
