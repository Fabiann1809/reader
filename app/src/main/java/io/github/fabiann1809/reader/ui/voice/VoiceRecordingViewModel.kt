package io.github.fabiann1809.reader.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.TranscribeAudio
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.data.voice.VoicePlayer
import io.github.fabiann1809.reader.data.voice.VoiceRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A voice note being recorded, listened to (T13.1), written down (T13.2) and saved in the open
 * book with an optional [tag] (T13.3).
 */
class VoiceRecordingViewModel(
    private val recorder: VoiceRecorder,
    private val player: VoicePlayer,
    private val transcribe: TranscribeAudio,
    private val noteRepository: NoteRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceRecordingUiState>(VoiceRecordingUiState.Idle)
    val uiState: StateFlow<VoiceRecordingUiState> = _uiState.asStateFlow()

    private val _tag = MutableStateFlow<NoteTag?>(null)

    /** Idea, Duda, Cita or Tarea: none until the user picks one. */
    val tag: StateFlow<NoteTag?> = _tag.asStateFlow()

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
        _tag.value = null
        _uiState.value = VoiceRecordingUiState.Recording(elapsedMillis = 0)
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                val level = recorder.level()
                _uiState.update { state ->
                    if (state !is VoiceRecordingUiState.Recording) return@update state
                    state.copy(elapsedMillis = now() - startedAt, levels = (state.levels + level).takeLast(WAVE_BARS))
                }
            }
        }
    }

    /** Picks [tag], or clears it when it was already picked. */
    fun toggleTag(tag: NoteTag) = _tag.update { if (it == tag) null else tag }

    /**
     * The check button: the recording and its transcript become a voice note of [bookId], at
     * [location] (the reader's Locator as JSON) and [page] when known.
     */
    fun save(bookId: Long, location: String?, page: Int?) {
        val recorded = _uiState.value as? VoiceRecordingUiState.Recorded ?: return
        val transcript = recorded.transcript as? Transcript.Ready ?: return
        player.stop()
        val note = Note(
            bookId = bookId,
            page = page,
            content = transcript.text.trim(),
            type = NoteType.VOICE,
            location = location,
            audioPath = recorded.path,
            tag = _tag.value,
            createdAt = now(),
        )
        // Saved at once so closing the sheet right away can't delete the recording.
        _uiState.value = VoiceRecordingUiState.Saved()
        viewModelScope.launch {
            val id = noteRepository.addNote(note)
            _uiState.update { if (it is VoiceRecordingUiState.Saved) VoiceRecordingUiState.Saved(id) else it }
        }
    }

    /** "Deshacer" in the snackbar: the note and its recording go away. */
    fun undoSave(noteId: Long) {
        viewModelScope.launch { noteRepository.getNote(noteId)?.let { noteRepository.deleteNote(it) } }
        savedShown()
    }

    /** The snackbar is gone: ready for the next recording. */
    fun savedShown() = _uiState.update { if (it is VoiceRecordingUiState.Saved) VoiceRecordingUiState.Idle else it }

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
        // Often enough for the wave to move smoothly and the seconds counter to never look stuck.
        const val TICK_MILLIS = 100L

        // The design's wave has 24 bars.
        const val WAVE_BARS = 24
    }
}
