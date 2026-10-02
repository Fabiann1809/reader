package io.github.fabiann1809.reader.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    data class Recorded(val path: String, val durationMillis: Long, val playing: Boolean = false) : VoiceRecordingUiState

    data class Failed(val reason: VoiceFailure) : VoiceRecordingUiState
}

enum class VoiceFailure { MICROPHONE_UNAVAILABLE, TOO_SHORT, PLAYBACK }

/** A voice note being recorded and listened to (T13.1); saving it as a note comes in T13.3. */
class VoiceRecordingViewModel(
    private val recorder: VoiceRecorder,
    private val player: VoicePlayer,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceRecordingUiState>(VoiceRecordingUiState.Idle)
    val uiState: StateFlow<VoiceRecordingUiState> = _uiState.asStateFlow()

    private var startedAt = 0L
    private var ticker: Job? = null

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
        _uiState.value = recorder.stop().fold(
            onSuccess = { path -> VoiceRecordingUiState.Recorded(path, duration) },
            onFailure = { VoiceRecordingUiState.Failed(VoiceFailure.TOO_SHORT) },
        )
    }

    fun togglePlayback() {
        val recorded = _uiState.value as? VoiceRecordingUiState.Recorded ?: return
        if (recorded.playing) {
            player.pause()
            _uiState.value = recorded.copy(playing = false)
            return
        }
        val played = player.play(recorded.path) {
            _uiState.update { state -> if (state is VoiceRecordingUiState.Recorded) state.copy(playing = false) else state }
        }
        _uiState.value = if (played.isSuccess) recorded.copy(playing = true) else VoiceRecordingUiState.Failed(VoiceFailure.PLAYBACK)
    }

    /** The trash button or closing the sheet: nothing is kept. */
    fun discard() {
        ticker?.cancel()
        recorder.cancel()
        discardRecorded()
        _uiState.value = VoiceRecordingUiState.Idle
    }

    private fun discardRecorded() {
        val recorded = _uiState.value as? VoiceRecordingUiState.Recorded ?: return
        player.stop()
        recorder.delete(recorded.path)
    }

    override fun onCleared() = discard()

    private companion object {
        // Often enough for a seconds counter to never look stuck.
        const val TICK_MILLIS = 250L
    }
}
