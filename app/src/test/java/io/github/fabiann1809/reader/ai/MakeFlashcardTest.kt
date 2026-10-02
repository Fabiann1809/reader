package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.testing.FakeAiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MakeFlashcardTest {

    private val aiProvider = FakeAiProvider()
    private val makeFlashcard = MakeFlashcard(aiProvider)

    @Test
    fun sendsTheTrimmedText() = runTest {
        makeFlashcard("  La entropía mide el desorden.  ").getOrThrow()

        assertEquals(listOf("La entropía mide el desorden."), aiProvider.flashcardRequests)
    }

    @Test
    fun blankOrTooLongTextFailsWithoutCallingTheAi() = runTest {
        assertTrue(makeFlashcard("   ").exceptionOrNull() is IllegalArgumentException)
        assertTrue(makeFlashcard("a".repeat(MAX_TEXT_LENGTH + 1)).exceptionOrNull() is IllegalArgumentException)
        assertTrue(aiProvider.flashcardRequests.isEmpty())
    }
}
