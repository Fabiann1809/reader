package io.github.fabiann1809.reader.ui.interpretation

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AnalyzeInterpretation
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class InterpretationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val aiProvider = FakeAiProvider()
    private val source = "La entropía mide el desorden de un sistema aislado."

    private fun viewModel() = InterpretationViewModel(source, AnalyzeInterpretation(aiProvider))

    @Test
    fun theOwnWordsAreAnalysedAgainstTheText() {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canAnalyze)

        viewModel.onOwnWordsChange("Que todo se desordena")
        viewModel.analyze()

        assertEquals(listOf(source to "Que todo se desordena"), aiProvider.interpretationRequests)
        val done = viewModel.uiState.value.analysis as AnalysisState.Done
        assertEquals("Captas la idea del desorden.", done.analysis.understood)
        assertEquals("Falta el sistema aislado.", done.analysis.incomplete)
    }

    @Test
    fun aFailureIsShownAndItCanBeAskedAgain() {
        aiProvider.analysis = Result.failure(AiError.NoInternet(IOException("offline")))
        val viewModel = viewModel()
        viewModel.onOwnWordsChange("Algo")

        viewModel.analyze()
        assertTrue((viewModel.uiState.value.analysis as AnalysisState.Failed).error is AiError.NoInternet)
        assertTrue(viewModel.uiState.value.canAnalyze)

        aiProvider.analysis = Result.success(InterpretationAnalysis(understood = "Bien"))
        viewModel.analyze()
        assertTrue(viewModel.uiState.value.analysis is AnalysisState.Done)
    }
}
