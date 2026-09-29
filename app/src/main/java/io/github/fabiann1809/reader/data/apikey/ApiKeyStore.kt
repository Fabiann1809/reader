package io.github.fabiann1809.reader.data.apikey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException

/** Stores the user's AI provider API key. Implementations must never log the key. */
interface ApiKeyStore {
    val hasApiKey: Flow<Boolean>

    suspend fun getApiKey(): String?

    suspend fun saveApiKey(apiKey: String)

    suspend fun clearApiKey()
}

/**
 * Keeps the API key encrypted with a Keystore-backed key (see [KeystoreCipher]).
 * Only the ciphertext is written to disk, in a Preferences DataStore file.
 */
class KeystoreApiKeyStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
    keyAlias: String = DEFAULT_KEY_ALIAS,
    // Tests pass their own scope and cancel it, so a new store can reopen the same file.
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : ApiKeyStore {

    // Only one DataStore instance may exist per file, so AppContainer must keep this store as a singleton.
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )
    private val cipher = KeystoreCipher(keyAlias)

    override val hasApiKey: Flow<Boolean> = dataStore.data.map { it[ENCRYPTED_API_KEY] != null }

    override suspend fun getApiKey(): String? {
        val encrypted = dataStore.data.first()[ENCRYPTED_API_KEY] ?: return null
        return withContext(Dispatchers.Default) {
            try {
                cipher.decrypt(encrypted)
            } catch (e: GeneralSecurityException) {
                // The Keystore key was lost (e.g. device restore): the stored value can't be recovered.
                null
            }
        } ?: run {
            clearApiKey()
            null
        }
    }

    override suspend fun saveApiKey(apiKey: String) {
        val encrypted = withContext(Dispatchers.Default) { cipher.encrypt(apiKey.trim()) }
        dataStore.edit { it[ENCRYPTED_API_KEY] = encrypted }
    }

    override suspend fun clearApiKey() {
        dataStore.edit { it.remove(ENCRYPTED_API_KEY) }
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "secure_settings"
        const val DEFAULT_KEY_ALIAS = "reader_api_key"
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
    }
}
