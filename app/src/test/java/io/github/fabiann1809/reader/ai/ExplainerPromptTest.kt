package io.github.fabiann1809.reader.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class ExplainerPromptTest {

    private val prompt = ExplainerPrompt.SYSTEM_INSTRUCTION

    @Test
    fun describesEveryFieldOfTheExplanation() {
        listOf("mainIdea", "simpleExplanation", "analogy", "keyTerms", "caveat")
            .forEach { field -> assertTrue(field, prompt.contains("$field:")) }
    }

    @Test
    fun includesTheSafetyRulesFromTheSpec() {
        assertTrue(prompt.contains("mismo idioma"))
        assertTrue(prompt.contains("No inventes información"))
        assertTrue(prompt.contains("ambiguo, está incompleto"))
        assertTrue(prompt.contains("sin formato Markdown"))
    }

    @Test
    fun hasNoLeftoverIndentation() {
        assertTrue(prompt.lines().none { it.startsWith(" ") })
    }
}
