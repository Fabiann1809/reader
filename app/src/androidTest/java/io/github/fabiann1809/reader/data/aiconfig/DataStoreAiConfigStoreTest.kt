package io.github.fabiann1809.reader.data.aiconfig

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.ai.AiConfig
import io.github.fabiann1809.reader.ai.AiProviderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreAiConfigStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileName = "test_ai_config"
    // Cancelled after each test so the next one can open the same file.
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var store: DataStoreAiConfigStore

    @Before
    fun setUp() {
        context.preferencesDataStoreFile(fileName).delete()
        store = DataStoreAiConfigStore(context, fileName, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        context.preferencesDataStoreFile(fileName).delete()
    }

    @Test
    fun startsWithGemini() = runTest {
        assertEquals(AiConfig(), store.config.first())
    }

    @Test
    fun savesTheProviderTheModelsAndTheAddress() = runTest {
        store.update {
            it.copy(
                provider = AiProviderKind.COMPATIBLE,
                models = mapOf(AiProviderKind.COMPATIBLE to "llama-3.3-70b", AiProviderKind.OPENAI to "gpt-4.1"),
                compatibleUrl = "https://api.groq.com/openai/v1",
            )
        }

        val saved = store.config.first()
        assertEquals(AiProviderKind.COMPATIBLE, saved.provider)
        assertEquals("llama-3.3-70b", saved.model)
        assertEquals("gpt-4.1", saved.modelOf(AiProviderKind.OPENAI))
        assertEquals("https://api.groq.com/openai/v1", saved.compatibleUrl)
    }

    @Test
    fun clearingAModelGoesBackToItsDefault() = runTest {
        store.update { it.copy(models = mapOf(AiProviderKind.GEMINI to "gemini-pro-latest")) }
        store.update { it.copy(models = emptyMap()) }

        assertEquals("gemini-flash-latest", store.config.first().model)
    }
}
