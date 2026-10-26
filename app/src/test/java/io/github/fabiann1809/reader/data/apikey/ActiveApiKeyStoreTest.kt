package io.github.fabiann1809.reader.data.apikey

import io.github.fabiann1809.reader.ai.AiProviderKind
import io.github.fabiann1809.reader.testing.FakeAiConfigStore
import io.github.fabiann1809.reader.testing.FakeProviderKeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveApiKeyStoreTest {

    private val keys = FakeProviderKeyStore()
    private val config = FakeAiConfigStore()
    private val store = ActiveApiKeyStore(keys, config)

    @Test
    fun savesTheKeyOfTheProviderInUse() = runTest {
        store.saveApiKey("gemini-key")

        assertEquals("gemini-key", keys.getApiKey(AiProviderKind.GEMINI))
        assertNull(keys.getApiKey(AiProviderKind.OPENAI))
    }

    @Test
    fun eachProviderKeepsItsOwnKey() = runTest {
        store.saveApiKey("gemini-key")
        config.update { it.copy(provider = AiProviderKind.OPENAI) }

        assertNull(store.getApiKey())
        assertFalse(store.hasApiKey.first())

        store.saveApiKey("openai-key")
        assertEquals("openai-key", store.getApiKey())

        config.update { it.copy(provider = AiProviderKind.GEMINI) }
        assertEquals("gemini-key", store.getApiKey())
        assertTrue(store.hasApiKey.first())
    }

    @Test
    fun clearingOnlyRemovesTheKeyInUse() = runTest {
        store.saveApiKey("gemini-key")
        config.update { it.copy(provider = AiProviderKind.ANTHROPIC) }
        store.saveApiKey("claude-key")

        store.clearApiKey()

        assertNull(store.getApiKey())
        assertEquals("gemini-key", keys.getApiKey(AiProviderKind.GEMINI))
    }
}
