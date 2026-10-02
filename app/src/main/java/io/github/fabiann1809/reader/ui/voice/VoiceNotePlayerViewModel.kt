package io.github.fabiann1809.reader.ui.voice

import androidx.lifecycle.ViewModel
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.voice.VoicePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which voice note of a list is playing, and which one could not be played. */
data class VoiceNotePlayback(val playingNoteId: Long? = null, val failedNoteId: Long? = null)

/** Plays the voice notes of a list (T13.5): the book's notes or "Todas las notas". One at a time. */
class VoiceNotePlayerViewModel(private val player: VoicePlayer) : ViewModel() {

    private val _playback = MutableStateFlow(VoiceNotePlayback())
    val playback: StateFlow<VoiceNotePlayback> = _playback.asStateFlow()

    /** Plays [note] (resuming it if it was paused), or pauses it when it is the one playing. */
    fun toggle(note: Note) {
        val path = note.audioPath ?: return
        if (_playback.value.playingNoteId == note.id) {
            pause()
            return
        }
        val played = player.play(path) {
            _playback.update { if (it.playingNoteId == note.id) it.copy(playingNoteId = null) else it }
        }
        _playback.value = if (played.isSuccess) VoiceNotePlayback(playingNoteId = note.id) else VoiceNotePlayback(failedNoteId = note.id)
    }

    /** Leaving the screen pauses whatever was playing. */
    fun pause() {
        player.pause()
        _playback.update { it.copy(playingNoteId = null) }
    }

    override fun onCleared() = player.stop()
}
