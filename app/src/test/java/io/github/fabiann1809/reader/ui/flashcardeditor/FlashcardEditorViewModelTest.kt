package io.github.fabiann1809.reader.ui.flashcardeditor

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.MakeFlashcard
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.FakeFlashcardRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class FlashcardEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeFlashcardRepository()
    private val aiProvider = FakeAiProvider()

    private fun viewModel(source: String? = null, page: Int? = null, tag: NoteTag? = null) =
        FlashcardEditorViewModel(bookId = 3, repository, MakeFlashcard(aiProvider), source, page, tag)

    @Test
    fun aCardWrittenFromScratchIsSavedInTheBook() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSave)
        assertNull(viewModel.uiState.value.source)

        viewModel.onFrontChange(" ¿Quién escribió Cosmos? ")
        viewModel.onBackChange("Carl Sagan")
        viewModel.onPageChange("12")
        viewModel.save()

        val card = repository.currentCards.single()
        assertEquals(3, card.bookId)
        assertEquals("¿Quién escribió Cosmos?", card.front)
        assertEquals("Carl Sagan", card.back)
        assertEquals(12, card.page)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun theAiProposesOnlyWhenAskedAndTheProposalCanBeEdited() {
        val viewModel = viewModel(source = "La entropía es una medida del desorden.", page = 42, tag = NoteTag.DOUBT)
        assertEquals("42", viewModel.uiState.value.page)
        assertTrue(aiProvider.flashcardRequests.isEmpty())

        viewModel.suggest()

        assertEquals(listOf("La entropía es una medida del desorden."), aiProvider.flashcardRequests)
        assertEquals("¿Qué mide la entropía?", viewModel.uiState.value.front)
        assertEquals("El desorden de un sistema.", viewModel.uiState.value.back)
        assertTrue(viewModel.uiState.value.isAiSuggested)

        viewModel.onBackChange("El desorden de un sistema aislado.")
        viewModel.save()

        val card = repository.currentCards.single()
        assertEquals("El desorden de un sistema aislado.", card.back)
        assertEquals(42, card.page)
        assertEquals(NoteTag.DOUBT, card.tag)
    }

    @Test
    fun aFailedProposalIsShownAndTheFieldsStayAsTheyWere() {
        aiProvider.flashcard = Result.failure(AiError.NoInternet(IOException("offline")))
        val viewModel = viewModel(source = "Texto")
        viewModel.onFrontChange("Mi pregunta")

        viewModel.suggest()

        assertTrue(viewModel.uiState.value.suggestionError is AiError.NoInternet)
        assertEquals("Mi pregunta", viewModel.uiState.value.front)
        assertFalse(viewModel.uiState.value.isAiSuggested)
    }

    @Test
    fun withoutASourceThereIsNothingToSuggest() {
        val viewModel = viewModel(source = "   ")

        viewModel.suggest()

        assertFalse(viewModel.uiState.value.canSuggest)
        assertTrue(aiProvider.flashcardRequests.isEmpty())
    }
}
