package io.github.fabiann1809.reader.ui.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.ui.AppViewModelProvider

/** What a list of notes needs to play its voice notes (T13.5), passed down as one value. */
data class VoiceNoteControls(
    val playback: VoiceNotePlayback = VoiceNotePlayback(),
    val onToggle: (Note) -> Unit = {},
) {
    fun isPlaying(note: Note): Boolean = playback.playingNoteId == note.id

    fun failed(note: Note): Boolean = playback.failedNoteId == note.id
}

/** The controls of a screen's [VoiceNotePlayerViewModel]; leaving the screen pauses the recording. */
@Composable
fun rememberVoiceNoteControls(
    viewModel: VoiceNotePlayerViewModel = viewModel(factory = AppViewModelProvider.Factory),
): VoiceNoteControls {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }
    return VoiceNoteControls(playback, viewModel::toggle)
}
