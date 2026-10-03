package io.github.fabiann1809.reader.ui.quiz

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.GenerateQuiz
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.testing.testQuiz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class QuizViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val aiProvider = FakeAiProvider()

    private fun viewModel(count: Int = 3) = QuizViewModel("Un capítulo", count, GenerateQuiz(aiProvider))

    private fun answering(viewModel: QuizViewModel) = viewModel.uiState.value as QuizUiState.Answering

    @Test
    fun eachAnswerIsShownAtOnceAndCountedOnce() {
        val viewModel = viewModel()
        assertEquals(listOf("Un capítulo" to 3), aiProvider.quizRequests)

        viewModel.next()
        assertEquals(0, answering(viewModel).index)

        // The right answer of the test quiz is always the second option.
        viewModel.choose(1)
        viewModel.choose(0)
        assertEquals(1, answering(viewModel).chosen)
        assertEquals(1, answering(viewModel).correctSoFar)

        viewModel.next()
        viewModel.choose(3)
        viewModel.next()
        viewModel.choose(1)
        assertTrue(answering(viewModel).isLast)
        viewModel.next()

        assertEquals(QuizUiState.Finished(correct = 2, total = 3), viewModel.uiState.value)
    }

    @Test
    fun aFailedQuizCanBeAskedAgain() {
        aiProvider.quiz = Result.failure(AiError.RateLimited())
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value is QuizUiState.Failed)

        aiProvider.quiz = Result.success(testQuiz(3))
        viewModel.retry()

        assertTrue(viewModel.uiState.value is QuizUiState.Answering)
        assertEquals(2, aiProvider.quizRequests.size)
    }

    @Test
    fun theOptionsShowRightWrongAndTheRestDimmed() {
        assertEquals(OptionLook.NORMAL, optionLook(option = 0, chosen = null, correct = 1))
        assertEquals(OptionLook.CORRECT, optionLook(option = 1, chosen = 0, correct = 1))
        assertEquals(OptionLook.WRONG, optionLook(option = 0, chosen = 0, correct = 1))
        assertEquals(OptionLook.DIMMED, optionLook(option = 2, chosen = 0, correct = 1))
        assertEquals(OptionLook.CORRECT, optionLook(option = 1, chosen = 1, correct = 1))
    }

    @Test
    fun noConnectionIsReported() {
        aiProvider.quiz = Result.failure(AiError.NoInternet(IOException("offline")))

        assertTrue((viewModel().uiState.value as QuizUiState.Failed).error is AiError.NoInternet)
    }
}
