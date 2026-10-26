package io.github.fabiann1809.reader.data.apikey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.fabiann1809.reader.ai.AiProviderKind
import io.github.fabiann1809.reader.data.aiconfig.AiConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException

/** Stores the user's API keys, one per AI provider. Implementations must never log a key. */
interface ProviderKeyStore {
    fun hasApiKey(provider: AiProviderKind): Flow<Boolean>

    suspend fun getApiKey(provider: AiProviderKind): String?

    suspend fun saveApiKey(provider: AiProviderKind, apiKey: String)

    suspend fun clearApiKey(provider: AiProviderKind)
}

/** The API key of the provider in use: what the screens and the providers work with. */
interface ApiKeyStore {
    val hasApiKey: Flow<Boolean>

    suspend fun getApiKey(): String?

    suspend fun saveApiKey(apiKey: String)

    suspend fun clearApiKey()
}

/** [ApiKeyStore] that follows the provider chosen in [config]: each provider keeps its own key. */
class ActiveApiKeyStore(private val keys: ProviderKeyStore, private val config: AiConfigStore) : ApiKeyStore {

    private suspend fun provider() = config.config.first().provider

    override val hasApiKey: Flow<Boolean> =
        config.config.map { it.provider }.distinctUntilChanged().flatMapLatest { keys.hasApiKey(it) }

    override suspend fun getApiKey(): String? = keys.getApiKey(provider())

    override suspend fun saveApiKey(apiKey: String) = keys.saveApiKey(provider(), apiKey)

    override suspend fun clearApiKey() = keys.clearApiKey(provider())
}

/**
 * Keeps the API keys encrypted with a Keystore-backed key (see [KeystoreCipher]).
 * Only the ciphertext is written to disk, in a Preferences DataStore file.
 */
class KeystoreApiKeyStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
    keyAlias: String = DEFAULT_KEY_ALIAS,
    // Tests pass their own scope and cancel it, so a new store can reopen the same file.
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : ProviderKeyStore {

    // Only one DataStore instance may exist per file, so AppContainer must keep this store as a singleton.
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )
    private val cipher = KeystoreCipher(keyAlias)

    override fun hasApiKey(provider: AiProviderKind): Flow<Boolean> = dataStore.data.map { it[slot(provider)] != null }

    override suspend fun getApiKey(provider: AiProviderKind): String? {
        val encrypted = dataStore.data.first()[slot(provider)] ?: return null
        return withContext(Dispatchers.Default) {
            try {
                cipher.decrypt(encrypted)
            } catch (e: GeneralSecurityException) {
                // The Keystore key was lost (e.g. device restore): the stored value can't be recovered.
                null
            }
        } ?: run {
            clearApiKey(provider)
            null
        }
    }

    override suspend fun saveApiKey(provider: AiProviderKind, apiKey: String) {
        val encrypted = withContext(Dispatchers.Default) { cipher.encrypt(apiKey.trim()) }
        dataStore.edit { it[slot(provider)] = encrypted }
    }

    override suspend fun clearApiKey(provider: AiProviderKind) {
        dataStore.edit { it.remove(slot(provider)) }
    }

    // Gemini was the only provider at first: its key keeps the name it was saved with.
    private fun slot(provider: AiProviderKind) = when (provider) {
        AiProviderKind.GEMINI -> stringPreferencesKey("encrypted_api_key")
        else -> stringPreferencesKey("encrypted_api_key_${provider.name.lowercase()}")
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "secure_settings"
        const val DEFAULT_KEY_ALIAS = "reader_api_key"
    }
}
