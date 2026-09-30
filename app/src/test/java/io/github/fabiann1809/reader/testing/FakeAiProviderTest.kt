package io.github.fabiann1809.reader.testing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAiProviderTest {

    @Test
    fun returnsConfiguredResultAndRecordsRequests() = runTest {
        val provider = FakeAiProvider(result = Result.success(testExplanation("Simple explanation")))

        val result = provider.explain("Complex paragraph")

        assertEquals("Simple explanation", result.getOrNull()?.mainIdea)
        assertEquals(listOf("Complex paragraph"), provider.requests)
    }

    @Test
    fun canSimulateFailures() = runTest {
        val provider = FakeAiProvider(result = Result.failure(IllegalStateException("boom")))

        assertTrue(provider.explain("text").isFailure)
    }
}
