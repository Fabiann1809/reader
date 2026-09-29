package io.github.fabiann1809.reader.ai.gemini

import kotlinx.serialization.Serializable

// Minimal subset of the Gemini REST API (models.generateContent). Unknown fields are ignored.

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null,
)

@Serializable
data class Part(
    val text: String? = null,
    // True for the model's internal reasoning parts, which must not be shown to the user.
    val thought: Boolean? = null,
)

@Serializable
data class GenerationConfig(
    val temperature: Double? = null,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null,
)
