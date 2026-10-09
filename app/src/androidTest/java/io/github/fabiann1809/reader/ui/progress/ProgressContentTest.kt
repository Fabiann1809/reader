package io.github.fabiann1809.reader.ui.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.prefs.ReadingGoal
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
class ProgressContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val stats = ProgressStats(
        todayMinutes = 24,
        todayPages = 0,
        goal = ReadingGoal(),
        streakDays = 6,
        week = DayOfWeek.entries.map { DayMinutes(it, if (it.value <= 3) 30 else 0) },
        finishedTitles = listOf("El arte de aprender", "Atención plena", "Hábitos atómicos"),
        year = 2026,
    )

    private fun setContent(uiState: ProgressUiState) {
        composeRule.setContent { ReaderTheme { ProgressContent(uiState) } }
    }

    @Test
    fun showsTheGoalStreakWeekAndFinishedBooks() {
        setContent(ProgressUiState(stats = stats))
        composeRule.onNodeWithText("Progreso").assertIsDisplayed()
        composeRule.onNodeWithText("Meta diaria").assertIsDisplayed()
        composeRule.onNodeWithText("6 días seguidos").assertIsDisplayed()
        composeRule.onNodeWithText("Esta semana").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Libros terminados").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingTheGoalOpensTheGoalDialog() {
        setContent(ProgressUiState(stats = stats))
        composeRule.onNodeWithText("Toca el anillo para cambiar").performClick()
        composeRule.onNodeWithText("Minutos al día").assertIsDisplayed()
    }

    @Test
    fun emptyStateWhenNothingWasRead() {
        setContent(ProgressUiState(stats = stats, isEmpty = true))
        composeRule.onNodeWithText("Empieza tu primera sesión").assertIsDisplayed()
    }
}
