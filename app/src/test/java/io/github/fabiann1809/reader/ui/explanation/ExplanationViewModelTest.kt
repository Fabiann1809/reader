package io.github.fabiann1809.reader.ui.explanation

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.ExplainText
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExplanationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sourceText = "La entropía es una medida del desorden de un sistema."

    private fun viewModel(provider: AiProvider) = ExplanationViewModel(sourceText, ExplainText(provider))

    @Test
    fun showsLoadingWhileTheAiAnswers() {
        val answer = CompletableDeferred<Result<String>>()
        val slowProvider = object : AiProvider {
            override suspend fun explain(text: String) = answer.await()
        }

        val viewModel = viewModel(slowProvider)
        assertEquals(ExplanationState.Loading, viewModel.uiState.value.explanation)

        answer.complete(Result.success("Idea central: el desorden aumenta."))
        assertEquals(
            ExplanationState.Success("Idea central: el desorden aumenta."),
            viewModel.uiState.value.explanation,
        )
    }

    @Test
    fun showsTheSourceTextAndTheExplanation() {
        val provider = FakeAiProvider(Result.success("Idea central: el desorden aumenta."))

        val state = viewModel(provider).uiState.value

        assertEquals(sourceText, state.sourceText)
        assertEquals(ExplanationState.Success("Idea central: el desorden aumenta."), state.explanation)
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

        provider.result = Result.success("Ahora sí")
        viewModel.retry()

        assertEquals(ExplanationState.Success("Ahora sí"), viewModel.uiState.value.explanation)
        assertEquals(2, provider.requests.size)
    }
}
