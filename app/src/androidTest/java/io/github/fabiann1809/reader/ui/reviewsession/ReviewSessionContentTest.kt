package io.github.fabiann1809.reader.ui.reviewsession

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.ReviewGrade
import io.github.fabiann1809.reader.ui.review.DueCard
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewSessionContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val cards = listOf(
        DueCard(Flashcard(id = 1, bookId = 1, front = "¿Qué es la entropía?", back = "Desorden"), "Física"),
        DueCard(Flashcard(id = 2, bookId = 1, front = "¿Qué es el calor?", back = "Energía"), "Física"),
    )

    @Test
    fun showsTheQuestionThenTheGradesAfterFlipping() {
        val state = mutableStateOf<ReviewSessionUiState>(ReviewSessionUiState.Reviewing(cards))
        var graded: ReviewGrade? = null
        composeRule.setContent {
            ReaderTheme {
                ReviewSessionContent(
                    uiState = state.value,
                    onFlip = { state.value = (state.value as ReviewSessionUiState.Reviewing).copy(flipped = true) },
                    onGrade = { graded = it },
                    onNavigateUp = {},
                )
            }
        }
        composeRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeRule.onNodeWithText("¿Qué es la entropía?").assertIsDisplayed()
        composeRule.onNodeWithText("Mostrar respuesta").performClick()
        composeRule.onNodeWithText("Desorden").assertIsDisplayed()
        composeRule.onNodeWithText("Bien").performClick()
        assertEquals(ReviewGrade.GOOD, graded)
    }

    @Test
    fun summaryShowsTheCountsAndClosesWithDone() {
        var done = false
        composeRule.setContent {
            ReaderTheme {
                ReviewSessionContent(
                    uiState = ReviewSessionUiState.Finished(reviewed = 3, correct = 2),
                    onFlip = {},
                    onGrade = {},
                    onNavigateUp = {},
                    onDone = { done = true },
                )
            }
        }
        composeRule.onNodeWithText("¡Sesión terminada!").assertIsDisplayed()
        composeRule.onNodeWithText("2 de 3").assertIsDisplayed()
        composeRule.onNodeWithText("Listo").performClick()
        assertEquals(true, done)
    }
}
