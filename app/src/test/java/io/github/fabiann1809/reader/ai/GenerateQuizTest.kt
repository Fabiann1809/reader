package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.testQuiz
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateQuizTest {

    private val aiProvider = FakeAiProvider()
    private val generateQuiz = GenerateQuiz(aiProvider)

    @Test
    fun asksForTheChosenNumberOfQuestions() = runTest {
        aiProvider.quiz = Result.success(testQuiz(5))

        val quiz = generateQuiz("  Un capítulo entero.  ", questionCount = 5).getOrThrow()

        assertEquals(listOf("Un capítulo entero." to 5), aiProvider.quizRequests)
        assertEquals(5, quiz.questions.size)
    }

    @Test
    fun anExtraQuestionIsDroppedAndBrokenOnesAreLeftOut() = runTest {
        val good = testQuiz(4).questions
        val broken = good.first().copy(options = listOf("Solo", "tres", "opciones"))
        aiProvider.quiz = Result.success(Quiz(listOf(broken) + good))

        val quiz = generateQuiz("Texto", questionCount = 3).getOrThrow()

        assertEquals(good.take(3), quiz.questions)
    }

    @Test
    fun tooFewUsableQuestionsIsNotAQuiz() = runTest {
        val outOfRange = testQuiz(3).questions.map { it.copy(correctIndex = 4) }
        aiProvider.quiz = Result.success(Quiz(outOfRange))

        assertTrue(generateQuiz("Texto", questionCount = 3).exceptionOrNull() is AiError.Unknown)
    }

    @Test
    fun invalidRequestsCostNoQuota() = runTest {
        assertTrue(generateQuiz("Texto", questionCount = 4).exceptionOrNull() is IllegalArgumentException)
        assertTrue(generateQuiz("  ", questionCount = 3).exceptionOrNull() is IllegalArgumentException)
        assertTrue(generateQuiz("a".repeat(MAX_QUIZ_TEXT_LENGTH + 1), questionCount = 3).exceptionOrNull() is IllegalArgumentException)
        assertTrue(aiProvider.quizRequests.isEmpty())
    }
}
