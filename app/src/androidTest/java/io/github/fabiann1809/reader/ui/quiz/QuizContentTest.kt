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

    private fun setContent(
        uiState: QuizUiState,
        onChoose: (Int) -> Unit = {},
        onClose: () -> Unit = {},
        onRepeat: () -> Unit = {},
        onSelect: (Int) -> Unit = {},
        onStart: () -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme {
                QuizContent("Física", uiState, onChoose, onNext = {}, onRetry = {}, onLeave = {}, onClose = onClose, onRepeat = onRepeat, onSelectCount = onSelect, onStart = onStart)
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
        setContent(QuizUiState.Finished(result, quiz), onClose = { closed = true })
        composeRule.onNodeWithText("1/2").assertIsDisplayed()
        composeRule.onNodeWithText("Entropía").assertIsDisplayed()
        composeRule.onNodeWithText("Calor").assertIsDisplayed()
        composeRule.onNodeWithText("Terminar").performClick()
        assertEquals(true, closed)
    }

    @Test
    fun introLetsPickAnEnabledSizeAndStart() {
        var selected = -1
        var started = false
        setContent(QuizUiState.Intro(selected = 3, maxSize = 5), onSelect = { selected = it }, onStart = { started = true })
        composeRule.onNodeWithText("5 preguntas").performClick()
        assertEquals(5, selected)
        composeRule.onNodeWithText("10 preguntas").assertIsNotEnabled()
        composeRule.onNodeWithText("Empezar").performClick()
        assertEquals(true, started)
    }

    @Test
    fun resultCanRepeatTheQuiz() {
        var repeated = false
        val result = QuizResult(1, 2, emptyList(), emptyList(), emptyList())
        setContent(QuizUiState.Finished(result, quiz), onRepeat = { repeated = true })
        composeRule.onNodeWithText("Repetir").performClick()
        assertEquals(true, repeated)
    }
}
