package io.github.fabiann1809.reader.ui.noteeditor

import io.github.fabiann1809.reader.data.note.Note
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

    private val existingNote = Note(
        id = 3,
        bookId = 7,
        page = 40,
        sourceText = "Original paragraph",
        content = "AI explanation",
        type = NoteType.EXPLANATION,
        createdAt = 1_000,
    )
    private val repository = FakeNoteRepository(listOf(existingNote))

    // Create lazily so they are built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private fun newNoteViewModel() = NoteEditorViewModel(bookId = 7, noteId = NEW_NOTE_ID, noteRepository = repository)

    private fun editViewModel(noteId: Long = 3) =
        NoteEditorViewModel(bookId = 7, noteId = noteId, noteRepository = repository)

    @Test
    fun cannotSaveBlankNote() {
        val viewModel = newNoteViewModel()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onContentChange("   ")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onContentChange("Una idea")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun pageAcceptsOnlyPositiveNumbers() {
        val viewModel = newNoteViewModel()
        viewModel.onContentChange("Una idea")

        viewModel.onPageChange("x1")
        assertEquals("", viewModel.uiState.value.page)

        viewModel.onPageChange("0")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onPageChange("15")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun saveNewNoteStoresManualNoteForTheBook() {
        val viewModel = newNoteViewModel()
        viewModel.onContentChange("  Somos polvo de estrellas ")
        viewModel.onPageChange("12")

        viewModel.save()

        val note = repository.currentNotes.last()
        assertEquals(7L, note.bookId)
        assertEquals("Somos polvo de estrellas", note.content)
        assertEquals(12, note.page)
        assertEquals(NoteType.MANUAL, note.type)
        assertNull(note.sourceText)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun editingLoadsExistingNote() {
        val state = editViewModel().uiState.value

        assertTrue(state.isEditing)
        assertFalse(state.isLoading)
        assertEquals("AI explanation", state.content)
        assertEquals("40", state.page)
    }

    @Test
    fun savingEditKeepsTypeSourceAndCreationDate() {
        val viewModel = editViewModel()
        viewModel.onContentChange("Edited explanation")
        viewModel.onPageChange("")

        viewModel.save()

        val stored = repository.currentNotes.single()
        assertEquals("Edited explanation", stored.content)
        assertNull(stored.page)
        assertEquals(NoteType.EXPLANATION, stored.type)
        assertEquals("Original paragraph", stored.sourceText)
        assertEquals(1_000L, stored.createdAt)
    }

    @Test
    fun deleteRemovesNoteAndClosesEditor() {
        val viewModel = editViewModel()

        viewModel.delete()

        assertTrue(repository.currentNotes.isEmpty())
        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun missingNoteClosesEditor() {
        assertTrue(editViewModel(noteId = 99).uiState.value.isDeleted)
    }
}
