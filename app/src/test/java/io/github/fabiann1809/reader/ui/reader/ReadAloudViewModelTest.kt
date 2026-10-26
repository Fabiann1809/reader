package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModelStore
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloud
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloudState
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloudStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.readium.r2.shared.publication.Locator

class ReadAloudViewModelTest {

    private class FakeReadAloud : ReadAloud {
        override val state = MutableStateFlow(ReadAloudState())
        override val sentence: StateFlow<Locator?> = MutableStateFlow(null)
        val calls = mutableListOf<String>()

        override suspend fun start(bookId: Long) {
            calls += "start $bookId"
            state.value = ReadAloudState(status = ReadAloudStatus.PLAYING)
        }

        override fun playPause() { calls += "playPause" }
        override fun previousSentence() { calls += "previous" }
        override fun nextSentence() { calls += "next" }
        override fun setSpeed(speed: Double) { calls += "speed $speed" }
        override fun setVoice(voiceId: String) { calls += "voice $voiceId" }
        override fun setSleepTimer(minutes: Int?) { calls += "timer $minutes" }
        override fun stop() { calls += "stop" }
    }

    private val readAloud = FakeReadAloud()
    private val viewModel get() = ReadAloudViewModel(bookId = 7, readAloud = readAloud)

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun startingReadsTheOpenBookAndShowsItsState() {
        val vm = viewModel

        vm.start()

        assertEquals(listOf("start 7"), readAloud.calls)
        assertEquals(ReadAloudStatus.PLAYING, vm.uiState.value.status)
    }

    @Test
    fun theControlsGoToTheReadAloud() {
        val vm = viewModel

        vm.playPause()
        vm.previousSentence()
        vm.nextSentence()
        vm.setSpeed(1.5)
        vm.setVoice("voz-1")
        vm.setSleepTimer(30)

        assertEquals(listOf("playPause", "previous", "next", "speed 1.5", "voice voz-1", "timer 30"), readAloud.calls)
    }

    @Test
    fun leavingTheReaderStopsTheReading() {
        val store = ViewModelStore().apply { put("reader", viewModel) }

        store.clear()

        assertEquals(listOf("stop"), readAloud.calls)
    }
}
