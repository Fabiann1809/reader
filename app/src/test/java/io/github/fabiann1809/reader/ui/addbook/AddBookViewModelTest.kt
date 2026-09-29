package io.github.fabiann1809.reader.ui.addbook

import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddBookViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()
    // Lazy so it is built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private val viewModel by lazy { AddBookViewModel(repository) }

    @Test
    fun cannotSaveUntilTitleAndAuthorAreFilled() {
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onTitleChange("Cosmos")
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onAuthorChange("Carl Sagan")
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun blankTitleIsNotValid() {
        viewModel.onTitleChange("   ")
        viewModel.onAuthorChange("Carl Sagan")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun totalPagesAcceptsOnlyDigits() {
        viewModel.onTotalPagesChange("12a")
        assertEquals("", viewModel.uiState.value.totalPages)

        viewModel.onTotalPagesChange("350")
        assertEquals("350", viewModel.uiState.value.totalPages)
    }

    @Test
    fun zeroPagesIsInvalid() {
        viewModel.onTitleChange("Cosmos")
        viewModel.onAuthorChange("Carl Sagan")
        viewModel.onTotalPagesChange("0")

        assertFalse(viewModel.uiState.value.isTotalPagesValid)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun saveStoresTrimmedBookAndMarksSaved() {
        viewModel.onTitleChange("  Cosmos ")
        viewModel.onAuthorChange(" Carl Sagan")
        viewModel.onTotalPagesChange("400")

        viewModel.save()

        val saved = repository.currentBooks.single()
        assertEquals("Cosmos", saved.title)
        assertEquals("Carl Sagan", saved.author)
        assertEquals(400, saved.totalPages)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun emptyTotalPagesIsStoredAsNull() {
        viewModel.onTitleChange("Dune")
        viewModel.onAuthorChange("Frank Herbert")

        viewModel.save()

        assertNull(repository.currentBooks.single().totalPages)
    }
}
