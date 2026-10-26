package io.github.fabiann1809.reader.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigTest {

    @Test
    fun startsWithGeminiAndItsDefaultModel() {
        val config = AiConfig()

        assertEquals(AiProviderKind.GEMINI, config.provider)
        assertEquals("gemini-flash-latest", config.model)
    }

    @Test
    fun eachProviderRemembersItsOwnModel() {
        val config = AiConfig(
            provider = AiProviderKind.OPENAI,
            models = mapOf(AiProviderKind.OPENAI to "gpt-4.1", AiProviderKind.GEMINI to "gemini-pro-latest"),
        )

        assertEquals("gpt-4.1", config.model)
        assertEquals("gemini-pro-latest", config.modelOf(AiProviderKind.GEMINI))
        assertEquals("claude-haiku-5-5", config.modelOf(AiProviderKind.ANTHROPIC))
    }

    @Test
    fun aBlankModelFallsBackToTheDefault() {
        val config = AiConfig(provider = AiProviderKind.OPENAI, models = mapOf(AiProviderKind.OPENAI to "  "))

        assertEquals("gpt-4.1-mini", config.model)
    }

    @Test
    fun theCompatibleProviderHasNoDefaultModel() {
        assertEquals("", AiConfig(provider = AiProviderKind.COMPATIBLE).model)
    }

    @Test
    fun onlyGeminiAndOpenAiTranscribe() {
        assertTrue(AiProviderKind.GEMINI.canTranscribe)
        assertTrue(AiProviderKind.OPENAI.canTranscribe)
        assertFalse(AiProviderKind.ANTHROPIC.canTranscribe)
        assertFalse(AiProviderKind.COMPATIBLE.canTranscribe)
    }

    @Test
    fun theDefaultModelIsAmongTheSuggestedOnes() {
        AiProviderKind.entries.filter { it.defaultModel.isNotEmpty() }.forEach {
            assertTrue(it.name, it.defaultModel in it.suggestedModels)
        }
    }
}
