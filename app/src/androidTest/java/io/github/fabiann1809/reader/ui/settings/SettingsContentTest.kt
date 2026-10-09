package io.github.fabiann1809.reader.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ui.components.SegmentedControl
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(
        uiState: SettingsUiState,
        onSaveKey: () -> Unit = {},
        onOpenPrivacy: () -> Unit = {},
    ) {
        composeRule.setContent {
            ReaderTheme {
                SettingsContent(
                    uiState = uiState,
                    onKeyInputChange = {},
                    onSaveKey = onSaveKey,
                    onClearKey = {},
                    onTestKey = {},
                    onMessageShown = {},
                    onOpenPrivacy = onOpenPrivacy,
                    onNavigateUp = {},
                )
            }
        }
    }

    @Test
    fun showsTheKeySectionFirstAndSavesATypedKey() {
        var saved = false
        setContent(SettingsUiState(keyInput = "AIza-123"), onSaveKey = { saved = true })
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule.onNodeWithText("Clave de API · Google Gemini").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar clave").assertIsEnabled().performClick()
        assertEquals(true, saved)
    }

    @Test
    fun clearAndTestNeedAStoredKey() {
        setContent(SettingsUiState(hasApiKey = false))
        composeRule.onNodeWithText("Borrar clave").assertIsNotEnabled()
        composeRule.onNodeWithText("Probar clave").assertIsNotEnabled()
    }

    @Test
    fun privacyCardOpensThePrivacyScreen() {
        var opened = false
        setContent(SettingsUiState(), onOpenPrivacy = { opened = true })
        composeRule.onNodeWithText("Privacidad").performScrollTo().performClick()
        assertEquals(true, opened)
    }

    @Test
    fun segmentedControlSelectsAnOption() {
        var selected = "b"
        composeRule.setContent {
            ReaderTheme { SegmentedControl(listOf("a", "b", "c"), selected, { it.uppercase() }, { selected = it }) }
        }
        composeRule.onNodeWithText("C").performClick()
        assertEquals("c", selected)
    }
}
