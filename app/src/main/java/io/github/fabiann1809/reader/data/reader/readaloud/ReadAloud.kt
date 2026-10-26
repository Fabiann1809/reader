package io.github.fabiann1809.reader.data.reader.readaloud

import io.github.fabiann1809.reader.data.prefs.ReadAloudSettings
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.shared.publication.Locator

/** What the read aloud is doing: [OFF] until it starts, [FAILED] when the phone's voice engine can't be used. */
enum class ReadAloudStatus { OFF, STARTING, PLAYING, PAUSED, FAILED }

/** How natural a voice sounds, from what the phone's voice engine says about it. */
enum class VoiceLevel { BASIC, GOOD, NATURAL }

/** A voice of the phone's engine for the book's language. [online] voices need an Internet connection. */
data class SpeechVoice(val id: String, val level: VoiceLevel, val online: Boolean)

/** What the reader's voice panel shows. [voices] are the book's language ones, the most natural first. */
data class ReadAloudState(
    val status: ReadAloudStatus = ReadAloudStatus.OFF,
    val speed: Double = ReadAloudSettings.NORMAL_SPEED,
    val voices: List<SpeechVoice> = emptyList(),
    val voiceId: String? = null,
    // The sleep timer: the reading stops after this many minutes; null = no timer.
    val sleepMinutes: Int? = null,
)

/** Reads the open book aloud (T11.15), sentence by sentence, from the page the reader is on. */
interface ReadAloud {
    val state: StateFlow<ReadAloudState>

    /** The sentence being read, which the page highlights; null when nothing is being read. */
    val sentence: StateFlow<Locator?>

    /** Starts reading the open [bookId] from where the reader is. Ends in [ReadAloudStatus.FAILED] if it can't. */
    suspend fun start(bookId: Long)

    fun playPause()

    fun previousSentence()

    fun nextSentence()

    fun setSpeed(speed: Double)

    fun setVoice(voiceId: String)

    /** Stops reading after [minutes]; null removes the timer. */
    fun setSleepTimer(minutes: Int?)

    fun stop()
}
