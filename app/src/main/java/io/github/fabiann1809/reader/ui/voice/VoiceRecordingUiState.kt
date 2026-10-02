package io.github.fabiann1809.reader.ui.voice

sealed interface VoiceRecordingUiState {
    /** Nothing recorded yet: waiting for the microphone permission. */
    data object Idle : VoiceRecordingUiState

    /** [levels] are the latest microphone levels (0 to 1), oldest first, for the wave. */
    data class Recording(val elapsedMillis: Long, val levels: List<Float> = emptyList()) : VoiceRecordingUiState

    data class Recorded(
        val path: String,
        val durationMillis: Long,
        val playing: Boolean = false,
        val transcript: Transcript = Transcript.Loading,
    ) : VoiceRecordingUiState {
        /** Saved once the transcript is there (even empty: the audio is the note then). */
        val canSave: Boolean get() = transcript is Transcript.Ready
    }

    /**
     * Saved as note [noteId] (null for the instant it is being written): the sheet closes and a
     * snackbar offers to undo it (T13.3).
     */
    data class Saved(val noteId: Long? = null) : VoiceRecordingUiState

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
