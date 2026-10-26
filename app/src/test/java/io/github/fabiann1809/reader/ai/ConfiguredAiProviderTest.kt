package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.testing.FakeAiConfigStore
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.testExplanation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguredAiProviderTest {

    private val config = FakeAiConfigStore()
    private val built = mutableListOf<AiConfig>()
    private val gemini = FakeAiProvider()
    private val openAi = FakeAiProvider(Result.success(testExplanation().copy(mainIdea = "De OpenAI")))
    private val provider = ConfiguredAiProvider(config) { chosen ->
        built += chosen
        if (chosen.provider == AiProviderKind.OPENAI) openAi else gemini
    }

    @Test
    fun usesTheProviderChosenAtTheMomentOfEachCall() = runTest {
        provider.explain("uno")
        config.update { it.copy(provider = AiProviderKind.OPENAI) }
        provider.explain("dos")

        assertEquals(listOf("uno"), gemini.requests)
        assertEquals(listOf("dos"), openAi.requests)
        assertEquals("De OpenAI", provider.explain("tres").getOrThrow().mainIdea)
    }

    @Test
    fun theChosenModelReachesTheProvider() = runTest {
        config.update { it.copy(provider = AiProviderKind.OPENAI, models = mapOf(AiProviderKind.OPENAI to "gpt-4.1")) }

        provider.explain("texto")

        assertEquals("gpt-4.1", built.single().model)
    }

    @Test
    fun everyOperationGoesToTheProviderInUse() = runTest {
        provider.refine("t", testExplanation(), ExplanationChange.SIMPLER)
        provider.makeFlashcard("t")
        provider.analyzeInterpretation("t", "mi idea")
        provider.generateQuiz("t", 3)
        provider.transcribe(byteArrayOf(1), "audio/mp4")

        assertEquals(1, gemini.refinements.size)
        assertEquals(listOf("t"), gemini.flashcardRequests)
        assertEquals(listOf("t" to "mi idea"), gemini.interpretationRequests)
        assertEquals(listOf("t" to 3), gemini.quizRequests)
        assertTrue(gemini.audios.size == 1)
    }
}
