package io.github.fabiann1809.reader.ui.review

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val cards = listOf(
        DueCard(Flashcard(id = 1, bookId = 1, front = "¿Qué es la entropía?", back = "Desorden"), "Física"),
        DueCard(Flashcard(id = 2, bookId = 1, front = "¿Qué es el calor?", back = "Energía"), "Física"),
    )

    private fun setContent(uiState: ReviewUiState, onStartSession: () -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                ReviewContent(uiState, onSelectMode = {}, onStartSession = onStartSession)
            }
        }
    }

    @Test
    fun todayCardShowsTheCountAndStartsTheSession() {
        var started = false
        setContent(ReviewUiState(isLoading = false, dueCards = cards), onStartSession = { started = true })
        composeRule.onNodeWithText("Para hoy").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("Empezar sesión").performClick()
        assertTrue(started)
    }

    @Test
    fun emptyStateWhenNothingIsDue() {
        setContent(ReviewUiState(isLoading = false))
        composeRule.onNodeWithText("Sin fichas por repasar").assertIsDisplayed()
    }
}
