package io.github.fabiann1809.reader.ui.voice

import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.testing.FakeVoicePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class VoiceNotePlayerViewModelTest {

    private val player = FakeVoicePlayer()
    private val viewModel = VoiceNotePlayerViewModel(player)

    private fun voiceNote(id: Long) =
        Note(id = id, bookId = 1, content = "Idea $id", type = NoteType.VOICE, audioPath = "voice/voice-$id.m4a")

    @Test
    fun playsANoteAndPausesItWithTheSameButton() {
        viewModel.toggle(voiceNote(1))
        assertEquals("voice/voice-1.m4a", player.playing)
        assertEquals(1L, viewModel.playback.value.playingNoteId)

        viewModel.toggle(voiceNote(1))
        assertNull(player.playing)
        assertNull(viewModel.playback.value.playingNoteId)
    }

    @Test
    fun anotherNoteTakesOverAndTheEndIsShown() {
        viewModel.toggle(voiceNote(1))
        viewModel.toggle(voiceNote(2))
        assertEquals(2L, viewModel.playback.value.playingNoteId)

        player.finish()

        assertNull(viewModel.playback.value.playingNoteId)
    }

    @Test
    fun aMissingRecordingIsMarkedOnItsNote() {
        player.playResult = Result.failure(IOException("deleted"))

        viewModel.toggle(voiceNote(3))

        assertEquals(VoiceNotePlayback(failedNoteId = 3), viewModel.playback.value)
    }

    @Test
    fun aNoteWithoutRecordingDoesNothing() {
        viewModel.toggle(Note(id = 4, bookId = 1, content = "Manual"))

        assertNull(player.playing)
        assertEquals(VoiceNotePlayback(), viewModel.playback.value)
    }
}
