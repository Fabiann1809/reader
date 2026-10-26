package io.github.fabiann1809.reader.data.apikey

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ai.AiProviderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreApiKeyStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileName = "test_secure_settings"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gemini = AiProviderKind.GEMINI
    private lateinit var store: KeystoreApiKeyStore

    @Before
    fun setUp() {
        context.preferencesDataStoreFile(fileName).delete()
        store = KeystoreApiKeyStore(context, fileName = fileName, keyAlias = "test_api_key", scope = scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        context.preferencesDataStoreFile(fileName).delete()
    }

    @Test
    fun startsWithoutKey() = runTest {
        assertNull(store.getApiKey(gemini))
        assertFalse(store.hasApiKey(gemini).first())
    }

    @Test
    fun savedKeyCanBeReadBack() = runTest {
        store.saveApiKey(gemini, "  sk-test-1234567890  ")

        assertEquals("sk-test-1234567890", store.getApiKey(gemini))
        assertTrue(store.hasApiKey(gemini).first())
    }

    @Test
    fun keyIsNotStoredInPlainText() = runTest {
        val apiKey = "sk-plaintext-check-ABCDEF"
        store.saveApiKey(gemini, apiKey)

        val fileContents = context.preferencesDataStoreFile(fileName).readBytes().toString(Charsets.ISO_8859_1)

        assertFalse(fileContents.contains(apiKey))
    }

    @Test
    fun clearRemovesKey() = runTest {
        store.saveApiKey(gemini, "sk-to-delete")

        store.clearApiKey(gemini)

        assertNull(store.getApiKey(gemini))
        assertFalse(store.hasApiKey(gemini).first())
    }

    @Test
    fun eachProviderHasItsOwnKey() = runTest {
        store.saveApiKey(gemini, "gemini-key")
        store.saveApiKey(AiProviderKind.OPENAI, "openai-key")

        assertEquals("gemini-key", store.getApiKey(gemini))
        assertEquals("openai-key", store.getApiKey(AiProviderKind.OPENAI))
        assertNull(store.getApiKey(AiProviderKind.ANTHROPIC))

        store.clearApiKey(AiProviderKind.OPENAI)
        assertEquals("gemini-key", store.getApiKey(gemini))
        assertFalse(store.hasApiKey(AiProviderKind.OPENAI).first())
    }

    @Test
    fun theKeySavedBeforeProvidersExistedStillWorksForGemini() = runTest {
        store.saveApiKey(gemini, "old-key")

        // A new store over the same file, as after an app update, reads the Gemini key it had.
        scope.cancel()
        val reopenedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val reopened = KeystoreApiKeyStore(context, fileName = fileName, keyAlias = "test_api_key", scope = reopenedScope)

        assertEquals("old-key", reopened.getApiKey(gemini))
        reopenedScope.cancel()
    }
}
