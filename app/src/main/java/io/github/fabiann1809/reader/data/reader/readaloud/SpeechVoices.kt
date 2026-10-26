package io.github.fabiann1809.reader.data.reader.readaloud

import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.Language

private typealias EngineVoice = AndroidTtsEngine.Voice

/**
 * The voices that speak [language] (any region of it), the best first: the exact region, then the
 * most natural one, then one that works offline. The robotic default voice is what makes reading
 * aloud tiring, so the engine's best voice is used unless the user picks another.
 */
@OptIn(ExperimentalReadiumApi::class)
fun bestVoicesFirst(language: Language?, voices: Collection<EngineVoice>): List<EngineVoice> =
    voices
        .filter { language == null || it.language.removeRegion() == language.removeRegion() }
        .sortedWith(
            compareByDescending<EngineVoice> { language != null && it.language == language }
                .thenByDescending { it.quality }
                .thenBy { it.requiresNetwork }
                .thenBy { it.id.value },
        )

/** The voice used when the user has not picked one. */
@OptIn(ExperimentalReadiumApi::class)
fun bestVoice(language: Language?, voices: Set<EngineVoice>): EngineVoice? = bestVoicesFirst(language, voices).firstOrNull()

@OptIn(ExperimentalReadiumApi::class)
fun EngineVoice.toSpeechVoice() = SpeechVoice(id = id.value, level = quality.toLevel(), online = requiresNetwork)

@OptIn(ExperimentalReadiumApi::class)
private fun AndroidTtsEngine.Voice.Quality.toLevel() = when (this) {
    AndroidTtsEngine.Voice.Quality.Lowest, AndroidTtsEngine.Voice.Quality.Low -> VoiceLevel.BASIC
    AndroidTtsEngine.Voice.Quality.Normal -> VoiceLevel.GOOD
    AndroidTtsEngine.Voice.Quality.High, AndroidTtsEngine.Voice.Quality.Highest -> VoiceLevel.NATURAL
}
