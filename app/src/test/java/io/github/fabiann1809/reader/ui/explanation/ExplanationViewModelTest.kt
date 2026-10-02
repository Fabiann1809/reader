package io.github.fabiann1809.reader.ui.explanation

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.ExplainText
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.FlashcardDraft
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.FakeNoteRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.testing.testExplanation
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExplanationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bookId = 7L
    private val sourceText = "La entropía es una medida del desorden de un sistema."
    private val noteRepository = FakeNoteRepository()
    private val labels = ExplanationLabels("Idea", "Simple", "Analogía", "Términos", "Ojo")

    private fun viewModel(provider: AiProvider) =
        ExplanationViewModel(bookId, sourceText, ExplainText(provider), noteRepository, labels)

    @Test
    fun showsLoadingWhileTheAiAnswers() {
        val answer = CompletableDeferred<Result<Explanation>>()
        val slowProvider = object : AiProvider {
            override suspend fun explain(text: String) = answer.await()

            override suspend fun transcribe(audio: ByteArray, mimeType: String) = Result.success("")

            override suspend fun makeFlashcard(text: String) = Result.success(FlashcardDraft("", ""))
        }

        val viewModel = viewModel(slowProvider)
        assertEquals(ExplanationState.Loading, viewModel.uiState.value.explanation)

        answer.complete(Result.success(testExplanation()))
        assertEquals(ExplanationState.Success(testExplanation()), viewModel.uiState.value.explanation)
    }

    @Test
    fun showsTheSourceTextAndTheExplanation() {
        val provider = FakeAiProvider(Result.success(testExplanation()))

        val state = viewModel(provider).uiState.value

        assertEquals(sourceText, state.sourceText)
        assertEquals(ExplanationState.Success(testExplanation()), state.explanation)
        assertEquals(listOf(sourceText), provider.requests)
    }

    @Test
    fun showsTheErrorWhenTheAiFails() {
        val error = AiError.RateLimited()

        val state = viewModel(FakeAiProvider(Result.failure(error))).uiState.value

        assertEquals(ExplanationState.Failed(error), state.explanation)
    }

    @Test
    fun retryAsksAgainAndShowsTheNewResult() {
        val provider = FakeAiProvider(Result.failure(AiError.Timeout(RuntimeException())))
        val viewModel = viewModel(provider)
        assertTrue(viewModel.uiState.value.explanation is ExplanationState.Failed)

        provider.result = Result.success(testExplanation("Ahora sí"))
        viewModel.retry()

        assertEquals(ExplanationState.Success(testExplanation("Ahora sí")), viewModel.uiState.value.explanation)
        assertEquals(2, provider.requests.size)
    }

    @Test
    fun saveAsNoteStoresTheExplanationAsTitledTextWithTheSource() {
        val viewModel = viewModel(FakeAiProvider(Result.success(testExplanation())))

        viewModel.saveAsNote()

        val note = noteRepository.currentNotes.single()
        assertEquals(bookId, note.bookId)
        assertEquals(NoteType.EXPLANATION, note.type)
        assertEquals(sourceText, note.sourceText)
        assertEquals(testExplanation().toPlainText(labels), note.content)
        assertEquals(SaveState.SAVED, viewModel.uiState.value.saveState)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun savingTwiceCreatesOnlyOneNote() {
        val viewModel = viewModel(FakeAiProvider(Result.success(testExplanation())))

        viewModel.saveAsNote()
        viewModel.saveAsNote()

        assertEquals(1, noteRepository.currentNotes.size)
    }

    @Test
    fun cannotSaveWhenThereIsNoExplanation() {
        val viewModel = viewModel(FakeAiProvider(Result.failure(AiError.RateLimited())))

        assertFalse(viewModel.uiState.value.canSave)
        viewModel.saveAsNote()

        assertTrue(noteRepository.currentNotes.isEmpty())
    }
}
