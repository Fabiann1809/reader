package io.github.fabiann1809.reader.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class ExplainerPromptTest {

    private val prompt = ExplainerPrompt.SYSTEM_INSTRUCTION

    @Test
    fun asksForTheFourFeynmanSections() {
        listOf(
            ExplainerPrompt.SECTION_MAIN_IDEA,
            ExplainerPrompt.SECTION_SIMPLE_EXPLANATION,
            ExplainerPrompt.SECTION_ANALOGY,
            ExplainerPrompt.SECTION_KEY_TERMS,
        ).forEach { section -> assertTrue(section, prompt.contains(section)) }
    }

    @Test
    fun includesTheSafetyRulesFromTheSpec() {
        assertTrue(prompt.contains("mismo idioma"))
        assertTrue(prompt.contains("No inventes información"))
        assertTrue(prompt.contains("ambiguo o está incompleto"))
        assertTrue(prompt.contains("sin formato Markdown"))
    }

    @Test
    fun hasNoLeftoverIndentation() {
        assertTrue(prompt.lines().none { it.startsWith(" ") })
    }
}
