package io.github.fabiann1809.reader.data.reader.readaloud

import android.app.Application
import io.github.fabiann1809.reader.data.prefs.ReadAloudPreferences
import io.github.fabiann1809.reader.data.prefs.ReadAloudSettings
import io.github.fabiann1809.reader.data.reader.ReaderSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.navigator.media.tts.AndroidTtsNavigator
import org.readium.navigator.media.tts.AndroidTtsNavigatorFactory
import org.readium.navigator.media.tts.TtsNavigator
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.navigator.media.tts.android.AndroidTtsPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Language
import org.readium.r2.shared.util.getOrElse

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * [ReadAloud] with Readium's TTS navigator on the phone's own voice engine. It keeps one reading
 * at a time and, like the reader's session, lives as long as the app: the reader's ViewModel
 * stops it when the reader is left.
 */
@OptIn(ExperimentalReadiumApi::class)
class ReadiumReadAloud(
    private val application: Application,
    private val session: ReaderSession,
    private val preferences: ReadAloudPreferences,
    private val scope: CoroutineScope,
) : ReadAloud {

    private val _state = MutableStateFlow(ReadAloudState())
    override val state: StateFlow<ReadAloudState> = _state.asStateFlow()

    private val _sentence = MutableStateFlow<Locator?>(null)
    override val sentence: StateFlow<Locator?> = _sentence.asStateFlow()

    private var navigator: AndroidTtsNavigator? = null
    private var language: Language? = null
    private var current = AndroidTtsPreferences()
    private var following: Job? = null
    private var sleepTimer: Job? = null

    override suspend fun start(bookId: Long) {
        stop()
        _state.value = ReadAloudState(status = ReadAloudStatus.STARTING)
        val saved = preferences.settings.first()
        val navigator = open(bookId, saved)
        if (navigator == null) {
            _state.value = ReadAloudState(status = ReadAloudStatus.FAILED)
            return
        }
        this.navigator = navigator
        applySavedVoice(navigator, saved.voiceId)
        follow(navigator, bookId)
        navigator.play()
    }

    private suspend fun open(bookId: Long, saved: ReadAloudSettings): AndroidTtsNavigator? {
        val publication = session.publication(bookId) ?: return null
        val factory = AndroidTtsNavigatorFactory(application, publication, voiceSelector = ::bestVoice) ?: return null
        current = AndroidTtsPreferences(speed = saved.speed)
        val listener = object : TtsNavigator.Listener {
            override fun onStopRequested() = stop()
        }
        return factory.createNavigator(listener, session.initialLocator(bookId), current).getOrElse { null }
    }

    // The voice the user picked, if the engine still has it; otherwise the engine's best one is already in use.
    private fun applySavedVoice(navigator: AndroidTtsNavigator, savedVoiceId: String?) {
        val language = navigator.settings.value.language.also { this.language = it }
        val voices = bestVoicesFirst(language, navigator.voices)
        if (savedVoiceId != null && voices.any { it.id.value == savedVoiceId }) {
            current = current.copy(voices = mapOf(language to AndroidTtsEngine.Voice.Id(savedVoiceId)))
            navigator.submitPreferences(current)
        }
        val inUse = navigator.settings.value.voices[language]?.value ?: voices.firstOrNull()?.id?.value
        _state.update { it.copy(speed = current.speed ?: it.speed, voices = voices.map { voice -> voice.toSpeechVoice() }, voiceId = inUse) }
    }

    // The page shows the sentence being read and follows it, which also saves the reading place.
    private fun follow(navigator: AndroidTtsNavigator, bookId: Long) {
        following = scope.launch {
            launch {
                navigator.location.collect { location ->
                    _sentence.value = location.utteranceLocator
                    session.jumpToLocation(bookId, location.utteranceLocator.toJSON().toString())
                }
            }
            launch {
                navigator.playback.collect { playback ->
                    when (playback.state) {
                        is TtsNavigator.State.Failure -> fail()
                        TtsNavigator.State.Ended -> stop()
                        else -> _state.update { it.copy(status = if (playback.playWhenReady) ReadAloudStatus.PLAYING else ReadAloudStatus.PAUSED) }
                    }
                }
            }
        }
    }

    override fun playPause() {
        val navigator = navigator ?: return
        if (_state.value.status == ReadAloudStatus.PLAYING) navigator.pause() else navigator.play()
    }

    override fun previousSentence() {
        navigator?.skipToPreviousUtterance()
    }

    override fun nextSentence() {
        navigator?.skipToNextUtterance()
    }

    override fun setSpeed(speed: Double) {
        current = current.copy(speed = speed)
        navigator?.submitPreferences(current)
        _state.update { it.copy(speed = speed) }
        scope.launch { preferences.setSpeed(speed) }
    }

    override fun setVoice(voiceId: String) {
        val language = language ?: return
        current = current.copy(voices = current.voices.orEmpty() + (language to AndroidTtsEngine.Voice.Id(voiceId)))
        navigator?.submitPreferences(current)
        _state.update { it.copy(voiceId = voiceId) }
        scope.launch { preferences.setVoice(voiceId) }
    }

    override fun setSleepTimer(minutes: Int?) {
        sleepTimer?.cancel()
        _state.update { it.copy(sleepMinutes = minutes) }
        if (minutes == null) return
        sleepTimer = scope.launch {
            delay(minutes * MILLIS_PER_MINUTE)
            stop()
        }
    }

    override fun stop() {
        following?.cancel()
        sleepTimer?.cancel()
        navigator?.close()
        navigator = null
        _sentence.value = null
        _state.value = ReadAloudState()
    }

    private fun fail() {
        stop()
        _state.value = ReadAloudState(status = ReadAloudStatus.FAILED)
    }
}
