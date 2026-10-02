package io.github.fabiann1809.reader.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.TranscribeAudio
import io.github.fabiann1809.reader.data.voice.VoicePlayer
import io.github.fabiann1809.reader.data.voice.VoiceRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface VoiceRecordingUiState {
    /** Nothing recorded yet: waiting for the microphone permission. */
    data object Idle : VoiceRecordingUiState

    data class Recording(val elapsedMillis: Long) : VoiceRecordingUiState

    data class Recorded(
        val path: String,
        val durationMillis: Long,
        val playing: Boolean = false,
        val transcript: Transcript = Transcript.Loading,
    ) : VoiceRecordingUiState

    data class Failed(val reason: VoiceFailure) : VoiceRecordingUiState
}

enum class VoiceFailure { MICROPHONE_UNAVAILABLE, TOO_SHORT, PLAYBACK }

/** The recording written down by the AI (T13.2), which the user can correct. */
sealed interface Transcript {
    data object Loading : Transcript

    /** An empty [text] means no words were heard. */
    data class Ready(val text: String) : Transcript

    data class Failed(val error: Throwable) : Transcript
}

/**
 * A voice note being recorded, listened to (T13.1) and written down (T13.2); saving it as a
 * note comes in T13.3.
 */
class VoiceRecordingViewModel(
    private val recorder: VoiceRecorder,
    private val player: VoicePlayer,
    private val transcribe: TranscribeAudio,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceRecordingUiState>(VoiceRecordingUiState.Idle)
    val uiState: StateFlow<VoiceRecordingUiState> = _uiState.asStateFlow()

    private var startedAt = 0L
    private var ticker: Job? = null
    private var transcription: Job? = null

    fun start() {
        if (_uiState.value is VoiceRecordingUiState.Recording) return
        discardRecorded()
        if (recorder.start().isFailure) {
            _uiState.value = VoiceRecordingUiState.Failed(VoiceFailure.MICROPHONE_UNAVAILABLE)
            return
        }
        startedAt = now()
        _uiState.value = VoiceRecordingUiState.Recording(elapsedMillis = 0)
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                _uiState.update { state ->
                    if (state is VoiceRecordingUiState.Recording) state.copy(elapsedMillis = now() - startedAt) else state
                }
            }
        }
    }

    fun stop() {
        if (_uiState.value !is VoiceRecordingUiState.Recording) return
        ticker?.cancel()
        val duration = now() - startedAt
        recorder.stop().fold(
            onSuccess = { path ->
                _uiState.value = VoiceRecordingUiState.Recorded(path, duration)
                transcribe(path)
            },
            onFailure = { _uiState.value = VoiceRecordingUiState.Failed(VoiceFailure.TOO_SHORT) },
        )
    }

    /** After a failed transcription (no connection, quota...): tries again with the same recording. */
    fun retryTranscription() {
        val recorded = _uiState.value as? VoiceRecordingUiState.Recorded ?: return
        if (recorded.transcript !is Transcript.Failed) return
        updateRecorded { it.copy(transcript = Transcript.Loading) }
        transcribe(recorded.path)
    }

    /** The user corrects the transcript. */
    fun onTranscriptChange(text: String) = updateRecorded { recorded ->
        if (recorded.transcript is Transcript.Ready) recorded.copy(transcript = Transcript.Ready(text)) else recorded
    }

    private fun transcribe(path: String) {
        transcription = viewModelScope.launch {
            val result = transcribe.invoke(path)
            updateRecorded { recorded ->
                if (recorded.path != path) return@updateRecorded recorded
                recorded.copy(transcript = result.fold({ Transcript.Ready(it) }, { Transcript.Failed(it) }))
            }
        }
    }

    private fun updateRecorded(change: (VoiceRecordingUiState.Recorded) -> VoiceRecordingUiState.Recorded) =
        _uiState.update { state -> if (state is VoiceRecordingUiState.Recorded) change(state) else state }

    fun togglePlayback() {
        val recorded = _uiState.value as? VoiceRecordingUiState.Recorded ?: return
        if (recorded.playing) {
            player.pause()
            updateRecorded { it.copy(playing = false) }
            return
        }
        val played = player.play(recorded.path) { updateRecorded { it.copy(playing = false) } }
        if (played.isSuccess) {
            updateRecorded { it.copy(playing = true) }
        } else {
            // A file that can't be played is no use: it goes, and the user can record again.
            discardRecorded()
            _uiState.value = VoiceRecordingUiState.Failed(VoiceFailure.PLAYBACK)
        }
    }

    /** The trash button or closing the sheet: nothing is kept. */
    fun discard() {
        ticker?.cancel()
        transcription?.cancel()
        recorder.cancel()
        discardRecorded()
        _uiState.value = VoiceRecordingUiState.Idle
    }

    private fun discardRecorded() {
        val recorded = _uiState.value as? VoiceRecordingUiState.Recorded ?: return
        transcription?.cancel()
        player.stop()
        recorder.delete(recorded.path)
    }

    override fun onCleared() = discard()

    private companion object {
        // Often enough for a seconds counter to never look stuck.
        const val TICK_MILLIS = 250L
    }
}
