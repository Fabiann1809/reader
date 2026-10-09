package io.github.fabiann1809.reader.ui.notes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllNotesContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val notes = listOf(
        NoteWithBook(Note(id = 1, bookId = 1, content = "El residuo dura minutos."), "El arte de aprender"),
        NoteWithBook(Note(id = 2, bookId = 1, content = "Cambiar de tarea cansa.", type = NoteType.EXPLANATION), "El arte de aprender"),
        NoteWithBook(Note(id = 3, bookId = 2, content = "Dudas del capítulo."), "Hábitos atómicos"),
    )

    private fun setContent(uiState: AllNotesUiState, onNoteClick: (Note) -> Unit = {}, onType: (NoteType?) -> Unit = {}) {
        composeRule.setContent {
            ReaderTheme {
                AllNotesContent(uiState, onNavigateUp = {}, onNoteClick = onNoteClick, onType = onType)
            }
        }
    }

    @Test
    fun notesAreGroupedByBookWithTheirCount() {
        setContent(AllNotesUiState(notes = notes, isLoading = false, hasNotes = true))
        composeRule.onNodeWithText("3 notas").assertIsDisplayed()
        composeRule.onNodeWithText("El arte de aprender").assertIsDisplayed()
        composeRule.onNodeWithText("2 notas").assertIsDisplayed()
        composeRule.onNodeWithText("Hábitos atómicos").assertIsDisplayed()
        composeRule.onNodeWithText("1 nota").assertIsDisplayed()
        composeRule.onNodeWithText("El residuo dura minutos.").assertIsDisplayed()
    }

    @Test
    fun tappingANoteOpensIt() {
        var opened: Long? = null
        setContent(AllNotesUiState(notes = notes, isLoading = false, hasNotes = true), onNoteClick = { opened = it.id })
        composeRule.onNodeWithText("Dudas del capítulo.").performClick()
        assertEquals(3L, opened)
    }

    @Test
    fun kindChipsFilter() {
        var type: NoteType? = null
        setContent(AllNotesUiState(notes = notes, isLoading = false, hasNotes = true), onType = { type = it })
        composeRule.onNodeWithText("Explicaciones").performClick()
        assertEquals(NoteType.EXPLANATION, type)
    }

    @Test
    fun emptyStateWithoutNotes() {
        setContent(AllNotesUiState(isLoading = false))
        composeRule.onNodeWithText("Aún no tienes notas").assertIsDisplayed()
    }
}
