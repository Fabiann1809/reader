package io.github.fabiann1809.reader.ui.voice

import io.github.fabiann1809.reader.testing.FakeVoicePlayer
import io.github.fabiann1809.reader.testing.FakeVoiceRecorder
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class VoiceRecordingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recorder = FakeVoiceRecorder()
    private val player = FakeVoicePlayer()
    private var clock = 1_000L
    private val viewModel = VoiceRecordingViewModel(recorder, player, now = { clock })

    @Test
    fun recordsUntilStoppedAndKeepsTheLength() {
        viewModel.start()
        assertTrue(recorder.recording)
        assertEquals(VoiceRecordingUiState.Recording(elapsedMillis = 0), viewModel.uiState.value)

        clock += 12_000
        viewModel.stop()

        assertFalse(recorder.recording)
        assertEquals(VoiceRecordingUiState.Recorded("/files/voice/voice-1.m4a", durationMillis = 12_000), viewModel.uiState.value)
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
        assertEquals("/files/voice/voice-1.m4a", player.playing)
        assertTrue((viewModel.uiState.value as VoiceRecordingUiState.Recorded).playing)

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
    }

    @Test
    fun discardingDeletesTheRecording() {
        viewModel.start()
        viewModel.stop()

        viewModel.discard()

        assertEquals(listOf("/files/voice/voice-1.m4a"), recorder.deleted)
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

        assertEquals(listOf("/files/voice/voice-1.m4a"), recorder.deleted)
        assertTrue(viewModel.uiState.value is VoiceRecordingUiState.Recording)
    }

    @Test
    fun theCounterShowsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:12", formatDuration(12_400))
        assertEquals("12:05", formatDuration(725_000))
    }
}
