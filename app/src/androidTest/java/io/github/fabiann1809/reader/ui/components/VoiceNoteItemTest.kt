package io.github.fabiann1809.reader.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceNoteItemTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeRule.activity.getString(id)

    private fun voiceNote(content: String) = Note(
        id = 1,
        bookId = 1,
        content = content,
        type = NoteType.VOICE,
        audioPath = "voice/voice-1.m4a",
        tag = NoteTag.DOUBT,
    )

    @Test
    fun aVoiceNoteShowsItsTagTranscriptAndPlaysFromTheList() {
        composeRule.setContent {
            var playing by remember { mutableStateOf(false) }
            ReaderTheme {
                NoteItem(
                    note = voiceNote("¿Por qué la entropía nunca baja?"),
                    onClick = {},
                    isPlaying = playing,
                    onTogglePlayback = { playing = !playing },
                )
            }
        }

        composeRule.onNodeWithText("${string(R.string.note_type_voice)} · ${string(R.string.note_tag_doubt)}").assertIsDisplayed()
        composeRule.onNodeWithText("¿Por qué la entropía nunca baja?").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.voice_play)).performClick()

        composeRule.onNodeWithContentDescription(string(R.string.voice_pause)).assertIsDisplayed()
    }

    @Test
    fun aVoiceNoteWithoutWordsSaysSo() {
        composeRule.setContent { ReaderTheme { NoteItem(note = voiceNote(""), onClick = {}) } }

        composeRule.onNodeWithText(string(R.string.note_voice_untranscribed)).assertIsDisplayed()
    }
}
