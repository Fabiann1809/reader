package io.github.fabiann1809.reader.ui.noteeditor

import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.testing.FakeNoteRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoteRepository()

    // Lazy so it is built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private val viewModel by lazy { NoteEditorViewModel(bookId = 7, noteRepository = repository) }

    @Test
    fun cannotSaveBlankNote() {
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onContentChange("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onContentChange("Una idea")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun pageAcceptsOnlyPositiveNumbers() {
        viewModel.onContentChange("Una idea")

        viewModel.onPageChange("x1")
        assertEquals("", viewModel.uiState.value.page)

        viewModel.onPageChange("0")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onPageChange("15")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun saveStoresManualNoteForTheBook() {
        viewModel.onContentChange("  Somos polvo de estrellas ")
        viewModel.onPageChange("12")

        viewModel.save()

        val note = repository.currentNotes.single()
        assertEquals(7L, note.bookId)
        assertEquals("Somos polvo de estrellas", note.content)
        assertEquals(12, note.page)
        assertEquals(NoteType.MANUAL, note.type)
        assertNull(note.sourceText)
        assertTrue(viewModel.uiState.value.isSaved)
    }
}
