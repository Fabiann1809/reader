package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.ai.AiConfig
import io.github.fabiann1809.reader.ai.AiProviderKind
import io.github.fabiann1809.reader.data.aiconfig.AiConfigStore
import io.github.fabiann1809.reader.data.apikey.ProviderKeyStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAiConfigStore(initial: AiConfig = AiConfig()) : AiConfigStore {
    override val config = MutableStateFlow(initial)

    override suspend fun update(change: (AiConfig) -> AiConfig) = config.update(change)
}

/** In-memory keys, one per provider (no encryption). */
class FakeProviderKeyStore : ProviderKeyStore {

    private val keys = MutableStateFlow<Map<AiProviderKind, String>>(emptyMap())

    override fun hasApiKey(provider: AiProviderKind): Flow<Boolean> = keys.map { provider in it }

    override suspend fun getApiKey(provider: AiProviderKind): String? = keys.value[provider]

    override suspend fun saveApiKey(provider: AiProviderKind, apiKey: String) = keys.update { it + (provider to apiKey.trim()) }

    override suspend fun clearApiKey(provider: AiProviderKind) = keys.update { it - provider }
}
