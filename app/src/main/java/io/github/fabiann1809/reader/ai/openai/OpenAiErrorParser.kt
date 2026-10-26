package io.github.fabiann1809.reader.ai.openai

import io.github.fabiann1809.reader.ai.AiError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Maps a failed response of an OpenAI-style API to an [AiError].
 *
 * Errors look like `{"error":{"message":"...","type":"insufficient_quota","code":"insufficient_quota"}}`.
 * The message is never copied: it could repeat part of what was sent.
 */
internal object OpenAiErrorParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(httpCode: Int, body: String): AiError {
        val error = errorObject(body)
        val code = error.text("code")
        val type = error.text("type")
        return when {
            httpCode == 401 || httpCode == 403 -> AiError.InvalidApiKey()
            code == "model_not_found" || httpCode == 404 -> AiError.ModelNotFound()
            httpCode == 429 && ("insufficient_quota" in listOf(code, type)) -> AiError.QuotaExhausted()
            httpCode == 429 -> AiError.RateLimited()
            httpCode >= 500 -> AiError.ServiceUnavailable(httpCode)
            else -> AiError.Unknown("The request failed with HTTP $httpCode")
        }
    }

    private fun errorObject(body: String): JsonObject? = try {
        json.parseToJsonElement(body).jsonObject["error"] as? JsonObject
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        // Not a JSON object (e.g. an HTML error page).
        null
    }

    private fun JsonObject?.text(name: String): String? = (this?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
}
