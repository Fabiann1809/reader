package io.github.fabiann1809.reader.ai.anthropic

import io.github.fabiann1809.reader.ai.AiError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Maps a failed Anthropic response to an [AiError].
 *
 * Errors look like `{"type":"error","error":{"type":"rate_limit_error","message":"..."}}`.
 * The message is read to tell an empty balance apart, but never copied.
 */
internal object AnthropicErrorParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(httpCode: Int, body: String): AiError {
        val error = errorObject(body)
        val message = error.text("message").orEmpty()
        return when {
            httpCode == 401 || httpCode == 403 -> AiError.InvalidApiKey()
            httpCode == 404 -> AiError.ModelNotFound()
            httpCode == 400 && "credit balance" in message -> AiError.QuotaExhausted()
            httpCode == 429 -> AiError.RateLimited()
            // 529 is "overloaded": the model is busy, not the key's fault.
            httpCode >= 500 -> AiError.ServiceUnavailable(httpCode)
            else -> AiError.Unknown("The request failed with HTTP $httpCode")
        }
    }

    private fun errorObject(body: String): JsonObject? = try {
        json.parseToJsonElement(body).jsonObject["error"] as? JsonObject
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun JsonObject?.text(name: String): String? = (this?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
}
