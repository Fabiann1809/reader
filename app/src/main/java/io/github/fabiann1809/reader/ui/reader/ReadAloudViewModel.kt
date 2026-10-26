package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloud
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloudState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The reader's voice panel (T11.15). Leaving the reader ends the reading: there is no background player. */
class ReadAloudViewModel(
    private val bookId: Long,
    private val readAloud: ReadAloud,
) : ViewModel() {

    val uiState: StateFlow<ReadAloudState> = readAloud.state

    fun start() {
        viewModelScope.launch { readAloud.start(bookId) }
    }

    fun playPause() = readAloud.playPause()

    fun previousSentence() = readAloud.previousSentence()

    fun nextSentence() = readAloud.nextSentence()

    fun setSpeed(speed: Double) = readAloud.setSpeed(speed)

    fun setVoice(voiceId: String) = readAloud.setVoice(voiceId)

    fun setSleepTimer(minutes: Int?) = readAloud.setSleepTimer(minutes)

    fun stop() = readAloud.stop()

    override fun onCleared() = readAloud.stop()
}
