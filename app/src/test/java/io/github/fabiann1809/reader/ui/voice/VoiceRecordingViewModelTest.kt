package io.github.fabiann1809.reader.ui.voice

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.TranscribeAudio
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.FakeVoicePlayer
import io.github.fabiann1809.reader.testing.FakeVoiceRecorder
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.IOException

class VoiceRecordingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // A real file: the transcription reads the recording before sending it.
    private val audioFile = File.createTempFile("voice", ".m4a").apply {
        writeBytes(byteArrayOf(1, 2, 3))
        deleteOnExit()
    }
    private val recorder = FakeVoiceRecorder().apply { stopResult = Result.success(audioFile.path) }
    private val player = FakeVoicePlayer()
    private val aiProvider = FakeAiProvider()
    private var clock = 1_000L
    private val viewModel = VoiceRecordingViewModel(recorder, player, TranscribeAudio(aiProvider, ioDispatcher = Dispatchers.Unconfined), now = { clock })

    private fun recorded() = viewModel.uiState.value as VoiceRecordingUiState.Recorded

    @Test
    fun recordsUntilStoppedAndKeepsTheLength() {
        viewModel.start()
        assertTrue(recorder.recording)
        assertEquals(VoiceRecordingUiState.Recording(elapsedMillis = 0), viewModel.uiState.value)

        clock += 12_000
        viewModel.stop()

        assertFalse(recorder.recording)
        assertEquals(audioFile.path, recorded().path)
        assertEquals(12_000, recorded().durationMillis)
    }

    @Test
    fun theRecordingIsWrittenDownAndCanBeCorrected() {
        viewModel.start()
        viewModel.stop()

        assertEquals(Transcript.Ready("Una idea sobre el capítulo"), recorded().transcript)
        assertEquals("audio/mp4", aiProvider.audios.single().second)

        viewModel.onTranscriptChange("Una idea sobre el capítulo 3")
        assertEquals(Transcript.Ready("Una idea sobre el capítulo 3"), recorded().transcript)
    }

    @Test
    fun aFailedTranscriptionCanBeRetried() {
        aiProvider.transcription = Result.failure(AiError.NoInternet(IOException("offline")))
        viewModel.start()
        viewModel.stop()
        assertTrue(recorded().transcript is Transcript.Failed)

        aiProvider.transcription = Result.success("Ahora sí")
        viewModel.retryTranscription()

        assertEquals(Transcript.Ready("Ahora sí"), recorded().transcript)
        assertEquals(2, aiProvider.audios.size)
    }

    @Test
    fun aBusyMicrophoneIsReported() {
        recorder.startResult = Result.failure(IllegalStateException("busy"))

        viewModel.start()

        assertEquals(VoiceRecordingUiState.Failed(VoiceFailure.MICROPHONE_UNAVAILABLE), viewModel.uiState.value)
    }

    @Test
    fun aRecordingWithoutAudioIsTooShort() {
        recorder.stopResult = Result.failure(RuntimeException("stop failed"))
        viewModel.start()

        viewModel.stop()

        assertEquals(VoiceRecordingUiState.Failed(VoiceFailure.TOO_SHORT), viewModel.uiState.value)
    }

    @Test
    fun theRecordingPlaysPausesAndEnds() {
        viewModel.start()
        viewModel.stop()

        viewModel.togglePlayback()
        assertEquals(audioFile.path, player.playing)
        assertTrue(recorded().playing)
        // Playing doesn't lose the transcript.
        assertTrue(recorded().transcript is Transcript.Ready)

        viewModel.togglePlayback()
        assertNull(player.playing)
        assertFalse((viewModel.uiState.value as VoiceRecordingUiState.Recorded).playing)

        viewModel.togglePlayback()
        player.finish()
        assertFalse((viewModel.uiState.value as VoiceRecordingUiState.Recorded).playing)
    }

    @Test
    fun anUnplayableFileIsReported() {
        player.playResult = Result.failure(IOException("damaged"))
        viewModel.start()
        viewModel.stop()

        viewModel.togglePlayback()

        assertEquals(VoiceRecordingUiState.Failed(VoiceFailure.PLAYBACK), viewModel.uiState.value)
        assertEquals(listOf(audioFile.path), recorder.deleted)
    }

    @Test
    fun discardingDeletesTheRecording() {
        viewModel.start()
        viewModel.stop()

        viewModel.discard()

        assertEquals(listOf(audioFile.path), recorder.deleted)
        assertEquals(VoiceRecordingUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun discardingWhileRecordingStopsTheMicrophone() {
        viewModel.start()

        viewModel.discard()

        assertFalse(recorder.recording)
        assertEquals(VoiceRecordingUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun recordingAgainThrowsAwayThePreviousTake() {
        viewModel.start()
        viewModel.stop()

        viewModel.start()

        assertEquals(listOf(audioFile.path), recorder.deleted)
        assertTrue(viewModel.uiState.value is VoiceRecordingUiState.Recording)
    }

    @Test
    fun theCounterShowsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:12", formatDuration(12_400))
        assertEquals("12:05", formatDuration(725_000))
    }
}
