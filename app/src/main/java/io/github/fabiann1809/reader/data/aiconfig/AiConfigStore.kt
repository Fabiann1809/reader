package io.github.fabiann1809.reader.data.aiconfig

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.fabiann1809.reader.ai.AiConfig
import io.github.fabiann1809.reader.ai.AiProviderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Which AI provider and model the user chose. Holds no secrets: the keys live in the key store. */
interface AiConfigStore {
    val config: Flow<AiConfig>

    suspend fun update(change: (AiConfig) -> AiConfig)
}

suspend fun AiConfigStore.current(): AiConfig = config.first()

// Tests pass their own [fileName] so they never touch the app's data.
class DataStoreAiConfigStore(
    context: Context,
    fileName: String = FILE_NAME,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : AiConfigStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val config: Flow<AiConfig> = dataStore.data.map { it.toConfig() }

    override suspend fun update(change: (AiConfig) -> AiConfig) {
        dataStore.edit { prefs ->
            val updated = change(prefs.toConfig())
            prefs[PROVIDER] = updated.provider.name
            prefs[COMPATIBLE_URL] = updated.compatibleUrl
            AiProviderKind.entries.forEach { kind ->
                val model = updated.models[kind]
                if (model.isNullOrBlank()) prefs.remove(modelKey(kind)) else prefs[modelKey(kind)] = model
            }
        }
    }

    private fun Preferences.toConfig() = AiConfig(
        provider = AiProviderKind.entries.find { it.name == this[PROVIDER] } ?: AiProviderKind.GEMINI,
        models = AiProviderKind.entries.mapNotNull { kind -> this[modelKey(kind)]?.let { kind to it } }.toMap(),
        compatibleUrl = this[COMPATIBLE_URL].orEmpty(),
    )

    private fun modelKey(kind: AiProviderKind) = stringPreferencesKey("model_${kind.name.lowercase()}")

    private companion object {
        const val FILE_NAME = "ai_config"
        val PROVIDER = stringPreferencesKey("provider")
        val COMPATIBLE_URL = stringPreferencesKey("compatible_url")
    }
}
