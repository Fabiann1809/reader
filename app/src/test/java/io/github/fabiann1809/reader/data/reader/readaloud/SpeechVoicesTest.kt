package io.github.fabiann1809.reader.data.reader.readaloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.navigator.media.tts.android.AndroidTtsEngine.Voice
import org.readium.navigator.media.tts.android.AndroidTtsEngine.Voice.Quality
import org.readium.r2.shared.util.Language

class SpeechVoicesTest {

    private fun voice(id: String, language: String, quality: Quality, network: Boolean = false) =
        Voice(Voice.Id(id), Language(language), quality, network)

    private val voices = setOf(
        voice("es-es-low", "es-ES", Quality.Low),
        voice("es-mx-high", "es-MX", Quality.High),
        voice("es-es-high-net", "es-ES", Quality.Highest, network = true),
        voice("es-es-high", "es-ES", Quality.Highest),
        voice("en-us-high", "en-US", Quality.Highest),
    )

    @Test
    fun theBestVoiceOfTheExactRegionComesFirstAndOfflineBeatsOnline() {
        val ids = bestVoicesFirst(Language("es-ES"), voices).map { it.id.value }

        assertEquals(listOf("es-es-high", "es-es-high-net", "es-es-low", "es-mx-high"), ids)
    }

    @Test
    fun withoutAVoiceOfTheExactRegionTheMostNaturalOfTheLanguageIsUsed() {
        assertEquals("es-es-high", bestVoice(Language("es-CO"), voices)?.id?.value)
    }

    @Test
    fun voicesOfOtherLanguagesAreLeftOut() {
        assertNull(bestVoice(Language("fr-FR"), voices))
    }

    @Test
    fun voicesAreGroupedInThreeLevelsForTheUser() {
        assertEquals(VoiceLevel.BASIC, voice("a", "es", Quality.Lowest).toSpeechVoice().level)
        assertEquals(VoiceLevel.GOOD, voice("a", "es", Quality.Normal).toSpeechVoice().level)
        assertEquals(VoiceLevel.NATURAL, voice("a", "es", Quality.High).toSpeechVoice().level)
        assertEquals(true, voice("a", "es", Quality.High, network = true).toSpeechVoice().online)
    }
}
