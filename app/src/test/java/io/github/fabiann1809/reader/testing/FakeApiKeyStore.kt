package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory ApiKeyStore for tests (no encryption). */
class FakeApiKeyStore(initialKey: String? = null) : ApiKeyStore {

    private val key = MutableStateFlow(initialKey)

    val currentKey: String? get() = key.value

    override val hasApiKey: Flow<Boolean> = key.map { it != null }

    override suspend fun getApiKey(): String? = key.value

    override suspend fun saveApiKey(apiKey: String) {
        key.value = apiKey.trim()
    }

    override suspend fun clearApiKey() {
        key.value = null
    }
}
