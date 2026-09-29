package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.ai.AiError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Maps a failed Gemini HTTP response to an [AiError].
 *
 * Gemini errors look like:
 * `{"error":{"code":400,"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"},
 *   {"violations":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}]}}`
 */
internal object GeminiErrorParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(httpCode: Int, body: String): AiError {
        val details = errorDetails(body)
        return when {
            httpCode == 401 || httpCode == 403 -> AiError.InvalidApiKey()
            "API_KEY_INVALID" in details.reasons -> AiError.InvalidApiKey()
            httpCode == 429 && details.quotaIds.any { "PerDay" in it } -> AiError.QuotaExhausted()
            httpCode == 429 -> AiError.RateLimited()
            httpCode >= 500 -> AiError.ServiceUnavailable(httpCode)
            else -> AiError.Unknown("Gemini request failed with HTTP $httpCode")
        }
    }

    private class ErrorDetails(val reasons: List<String>, val quotaIds: List<String>)

    private fun errorDetails(body: String): ErrorDetails {
        val detailList = try {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("details") as? JsonArray
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            // Body is not a JSON object (e.g. an HTML error page).
            null
        } ?: return ErrorDetails(emptyList(), emptyList())

        val reasons = detailList.mapNotNull { it.stringField("reason") }
        val quotaIds = detailList
            .flatMap { (it as? JsonObject)?.get("violations") as? JsonArray ?: JsonArray(emptyList()) }
            .mapNotNull { it.stringField("quotaId") }
        return ErrorDetails(reasons, quotaIds)
    }

    private fun JsonElement.stringField(name: String): String? =
        ((this as? JsonObject)?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
}
