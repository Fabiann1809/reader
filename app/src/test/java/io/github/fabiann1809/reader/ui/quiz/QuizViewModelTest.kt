package io.github.fabiann1809.reader.ui.quiz

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.GenerateQuiz
import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.FakeFlashcardRepository
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
    private val cards = FakeFlashcardRepository()

    private fun viewModel(count: Int = 3) = QuizViewModel(bookId = 7, "Un capítulo", count, GenerateQuiz(aiProvider), cards)

    private fun answering(viewModel: QuizViewModel) = viewModel.uiState.value as QuizUiState.Answering

    private fun finished(viewModel: QuizViewModel) = viewModel.uiState.value as QuizUiState.Finished

    /** Answers the test quiz's questions in order; its right answer is always option 1. */
    private fun answer(viewModel: QuizViewModel, vararg options: Int) = options.forEach { option ->
        viewModel.choose(option)
        viewModel.next()
    }

    @Test
    fun eachAnswerIsShownAtOnceAndCountedOnce() {
        val viewModel = viewModel()
        assertEquals(listOf("Un capítulo" to 3), aiProvider.quizRequests)

        viewModel.next()
        assertEquals(0, answering(viewModel).index)

        viewModel.choose(1)
        viewModel.choose(0)
        assertEquals(1, answering(viewModel).chosen)
        viewModel.next()
        answer(viewModel, 3, 1)

        assertEquals(2, finished(viewModel).result.correct)
        assertEquals(3, finished(viewModel).result.total)
    }

    @Test
    fun theMissedQuestionsBecomeCardsOfTheBookOnce() {
        val viewModel = viewModel()
        answer(viewModel, 1, 0, 2)

        viewModel.createCardsFromMistakes()
        viewModel.createCardsFromMistakes()

        assertTrue(finished(viewModel).cardsCreated)
        assertEquals(listOf("¿Pregunta 2?", "¿Pregunta 3?"), cards.currentCards.map { it.front })
        assertTrue(cards.currentCards.all { it.bookId == 7L && it.back == "B. Porque lo dice el texto." })
    }

    @Test
    fun anAnswerEndingInAPeriodDoesNotGetTwo() {
        aiProvider.quiz = Result.success(Quiz(testQuiz(3).questions.map { it.copy(options = listOf("A.", "B.", "C.", "D.")) }))
        val viewModel = viewModel()
        answer(viewModel, 0, 1, 1)

        viewModel.createCardsFromMistakes()

        assertEquals("B. Porque lo dice el texto.", cards.currentCards.single().back)
    }

    @Test
    fun theResultSplitsTopicsIntoStrongAndWeak() {
        val questions = testQuiz(4).questions
        val quiz = Quiz(
            listOf(
                questions[0].copy(topic = "Entropía"),
                questions[1].copy(topic = "Entropía"),
                questions[2].copy(topic = "Calor"),
                questions[3].copy(topic = ""),
            ),
        )

        val result = QuizResult.of(quiz, answers = listOf(1, 1, 0, 0))

        assertEquals(listOf("Entropía"), result.strongTopics)
        assertEquals(listOf("Calor"), result.weakTopics)
        assertEquals(2, result.missed.size)
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
