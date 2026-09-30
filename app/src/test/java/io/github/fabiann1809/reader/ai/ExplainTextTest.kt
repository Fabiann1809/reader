package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.testing.FakeAiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplainTextTest {

    private val provider = FakeAiProvider(result = Result.success("Idea central: la luz es energía."))
    private val explainText = ExplainText(provider)

    @Test
    fun returnsTheProviderExplanation() = runTest {
        val result = explainText("La luz es una forma de energía.")

        assertEquals("Idea central: la luz es energía.", result.getOrNull())
    }

    @Test
    fun sendsTheFragmentWithoutSurroundingWhitespace() = runTest {
        explainText("  \nLa luz es una forma de energía.\n  ")

        assertEquals(listOf("La luz es una forma de energía."), provider.requests)
    }

    @Test
    fun passesProviderErrorsThrough() = runTest {
        val error = AiError.QuotaExhausted()
        provider.result = Result.failure(error)

        assertSame(error, explainText("text").exceptionOrNull())
    }

    @Test
    fun blankTextFailsWithoutCallingTheProvider() = runTest {
        val result = explainText("   \n ")

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(provider.requests.isEmpty())
    }

    @Test
    fun tooLongTextFailsWithoutCallingTheProvider() = runTest {
        val result = explainText("a".repeat(MAX_TEXT_LENGTH + 1))

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(provider.requests.isEmpty())
    }

    @Test
    fun acceptsTextAtTheLimit() = runTest {
        assertTrue(explainText("a".repeat(MAX_TEXT_LENGTH)).isSuccess)
    }
}
