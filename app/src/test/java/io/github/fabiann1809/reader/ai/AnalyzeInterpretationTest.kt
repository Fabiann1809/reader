package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.testing.FakeAiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeInterpretationTest {

    private val aiProvider = FakeAiProvider()
    private val analyze = AnalyzeInterpretation(aiProvider)

    @Test
    fun sendsBothTextsTrimmed() = runTest {
        analyze("  La entropía mide el desorden. ", " Habla del desorden ").getOrThrow()

        assertEquals(listOf("La entropía mide el desorden." to "Habla del desorden"), aiProvider.interpretationRequests)
    }

    @Test
    fun withoutOwnWordsOrWithTooMuchTextNothingIsSent() = runTest {
        assertTrue(analyze("Texto", "   ").exceptionOrNull() is IllegalArgumentException)
        assertTrue(analyze("Texto", "a".repeat(MAX_TEXT_LENGTH + 1)).exceptionOrNull() is IllegalArgumentException)
        assertTrue(aiProvider.interpretationRequests.isEmpty())
    }
}
