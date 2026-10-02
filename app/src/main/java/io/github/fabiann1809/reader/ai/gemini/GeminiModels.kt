package io.github.fabiann1809.reader.ai.gemini

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
    // A file sent inside the request, like a voice note's audio.
    val inlineData: InlineData? = null,
)

@Serializable
data class InlineData(
    val mimeType: String,
    // The file's bytes in Base64.
    val data: String,
)

@Serializable
data class GenerationConfig(
    val temperature: Double? = null,
    // "application/json" plus a schema makes the model answer with JSON of exactly that shape.
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
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
