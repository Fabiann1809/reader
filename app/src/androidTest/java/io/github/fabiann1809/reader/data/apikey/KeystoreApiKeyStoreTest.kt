package io.github.fabiann1809.reader.data.apikey

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        assertNull(store.getApiKey())
        assertFalse(store.hasApiKey.first())
    }

    @Test
    fun savedKeyCanBeReadBack() = runTest {
        store.saveApiKey("  sk-test-1234567890  ")

        assertEquals("sk-test-1234567890", store.getApiKey())
        assertTrue(store.hasApiKey.first())
    }

    @Test
    fun keyIsNotStoredInPlainText() = runTest {
        val apiKey = "sk-plaintext-check-ABCDEF"
        store.saveApiKey(apiKey)

        val fileContents = context.preferencesDataStoreFile(fileName).readBytes().toString(Charsets.ISO_8859_1)

        assertFalse(fileContents.contains(apiKey))
    }

    @Test
    fun clearRemovesKey() = runTest {
        store.saveApiKey("sk-to-delete")

        store.clearApiKey()

        assertNull(store.getApiKey())
        assertFalse(store.hasApiKey.first())
    }
}
