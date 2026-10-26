package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.data.aiconfig.AiConfigStore
import io.github.fabiann1809.reader.data.aiconfig.current

/**
 * The [AiProvider] the rest of the app talks to: on each call it uses the provider and model the
 * user has chosen at that moment, so changing them in Ajustes takes effect without restarting.
 */
class ConfiguredAiProvider(
    private val configStore: AiConfigStore,
    private val create: (AiConfig) -> AiProvider,
) : AiProvider {

    private suspend fun active(): AiProvider = create(configStore.current())

    override suspend fun explain(text: String) = active().explain(text)

    override suspend fun refine(text: String, previous: Explanation, change: ExplanationChange) =
        active().refine(text, previous, change)

    override suspend fun transcribe(audio: ByteArray, mimeType: String) = active().transcribe(audio, mimeType)

    override suspend fun makeFlashcard(text: String) = active().makeFlashcard(text)

    override suspend fun analyzeInterpretation(text: String, interpretation: String) =
        active().analyzeInterpretation(text, interpretation)

    override suspend fun generateQuiz(text: String, questionCount: Int) = active().generateQuiz(text, questionCount)
}
