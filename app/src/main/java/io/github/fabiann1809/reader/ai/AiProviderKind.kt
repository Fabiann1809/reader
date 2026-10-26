package io.github.fabiann1809.reader.ai

/** The AI services the user can bring their own key for (T20.x). */
enum class AiProviderKind(
    /** The model used until the user picks another one; empty when the user must say which. */
    val defaultModel: String,
    /** A short list of models to pick from; any other name can be typed. */
    val suggestedModels: List<String>,
) {
    GEMINI("gemini-flash-latest", listOf("gemini-flash-latest", "gemini-flash-lite-latest", "gemini-pro-latest")),
    OPENAI("gpt-4.1-mini", listOf("gpt-4.1-mini", "gpt-4.1", "gpt-4o-mini")),
    ANTHROPIC("claude-haiku-5-5", listOf("claude-haiku-5-5", "claude-sonnet-5-5", "claude-opus-5-5")),

    /** Any service that speaks OpenAI's chat API (OpenRouter, Groq, DeepSeek...): the user gives its address and model. */
    COMPATIBLE("", emptyList()),
    ;

    /** Whether the provider can write down a voice note; the others save it but can't transcribe it. */
    val canTranscribe: Boolean get() = this == GEMINI || this == OPENAI
}

/**
 * Which provider and model are in use. [models] keeps the model chosen for each provider, so
 * switching provider and back does not lose it.
 */
data class AiConfig(
    val provider: AiProviderKind = AiProviderKind.GEMINI,
    val models: Map<AiProviderKind, String> = emptyMap(),
    /** Base address of the compatible service (e.g. `https://openrouter.ai/api/v1`). */
    val compatibleUrl: String = "",
) {
    val model: String get() = modelOf(provider)

    fun modelOf(kind: AiProviderKind): String = models[kind]?.takeIf { it.isNotBlank() } ?: kind.defaultModel
}
