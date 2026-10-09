package io.github.fabiann1809.reader.ui.quiz

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizQuestion
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val quiz = Quiz(
        listOf(
            QuizQuestion("¿Qué es la entropía?", listOf("Orden", "Desorden", "Calor", "Frío"), 1, "Mide el desorden.", "Entropía"),
            QuizQuestion("¿Qué es el calor?", listOf("Energía", "Masa", "Luz", "Sonido"), 0, "Es energía.", "Calor"),
        ),
    )

    private fun setContent(uiState: QuizUiState, onChoose: (Int) -> Unit = {}, onClose: () -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                QuizContent("Física", uiState, onChoose, onNext = {}, onRetry = {}, onLeave = {}, onClose = onClose)
            }
        }
    }

    @Test
    fun questionShowsItsCounterAndOptions() {
        var chosen = -1
        setContent(QuizUiState.Answering(quiz), onChoose = { chosen = it })
        composeRule.onNodeWithText("Física · Pregunta 1/2").assertIsDisplayed()
        composeRule.onNodeWithText("¿Qué es la entropía?").assertIsDisplayed()
        composeRule.onNodeWithText("Siguiente").assertIsNotEnabled()
        composeRule.onNodeWithText("Desorden").performClick()
        assertEquals(1, chosen)
    }

    @Test
    fun answeredQuestionShowsFeedbackAndEnablesNext() {
        setContent(QuizUiState.Answering(quiz, chosen = 0))
        composeRule.onNodeWithText("No exactamente").assertIsDisplayed()
        composeRule.onNodeWithText("Mide el desorden.").assertIsDisplayed()
        composeRule.onNodeWithText("Siguiente").assertIsEnabled()
    }

    @Test
    fun resultShowsScoreAndTopics() {
        var closed = false
        val result = QuizResult(1, 2, listOf("Entropía"), listOf("Calor"), listOf(quiz.questions[1]))
        setContent(QuizUiState.Finished(result), onClose = { closed = true })
        composeRule.onNodeWithText("1/2").assertIsDisplayed()
        composeRule.onNodeWithText("Entropía").assertIsDisplayed()
        composeRule.onNodeWithText("Calor").assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar").performClick()
        assertEquals(true, closed)
    }
}
