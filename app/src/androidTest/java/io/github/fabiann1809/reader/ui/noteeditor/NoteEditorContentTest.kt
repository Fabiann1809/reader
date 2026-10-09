package io.github.fabiann1809.reader.ui.noteeditor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteEditorContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(uiState: NoteEditorUiState, onSave: () -> Unit = {}, onDelete: () -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                NoteEditorContent(
                    uiState = uiState,
                    onContentChange = {},
                    onPageChange = {},
                    onSave = onSave,
                    onDelete = onDelete,
                    onNavigateUp = {},
                )
            }
        }
    }

    @Test
    fun aNewNoteCannotBeSavedWithoutText() {
        setContent(NoteEditorUiState())
        composeRule.onNodeWithText("Nueva nota").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar").assertIsNotEnabled()
    }

    @Test
    fun savingCallsBack() {
        var saved = false
        setContent(NoteEditorUiState(content = "Una idea"), onSave = { saved = true })
        composeRule.onNodeWithText("Guardar").assertIsEnabled().performClick()
        assertEquals(true, saved)
    }

    @Test
    fun editingAsksBeforeDeleting() {
        var deleted = false
        setContent(NoteEditorUiState(isEditing = true, content = "Una idea"), onDelete = { deleted = true })
        composeRule.onNodeWithText("Editar nota").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Eliminar nota").performClick()
        assertEquals(false, deleted)
        composeRule.onNodeWithText("Eliminar").performClick()
        assertEquals(true, deleted)
    }
}
