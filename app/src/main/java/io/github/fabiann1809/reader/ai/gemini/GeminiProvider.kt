package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * [AiProvider] backed by Google Gemini (REST `models.generateContent`).
 *
 * The API key is read from [apiKeyStore] on every call and sent only in the
 * `x-goog-api-key` header. Nothing here logs requests, headers or the key.
 * The answer is requested as JSON with [ExplanationSchema] and decoded into an [Explanation].
 * Every failure is reported as an [AiError]. When [model] is overloaded (5xx), the request is
 * retried once with [fallbackModel] so the user still gets an answer.
 */
class GeminiProvider(
    private val apiKeyStore: ApiKeyStore,
    private val systemInstruction: String,
    private val httpClient: OkHttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = DEFAULT_MODEL,
    private val fallbackModel: String? = DEFAULT_FALLBACK_MODEL,
) : AiProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun explain(text: String): Result<Explanation> {
        val apiKey = apiKeyStore.getApiKey() ?: return Result.failure(AiError.MissingApiKey())

        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            generationConfig = GenerationConfig(
                temperature = TEMPERATURE,
                responseMimeType = "application/json",
                responseSchema = ExplanationSchema,
            ),
        )
        return withContext(Dispatchers.IO) {
            val result = call(model, apiKey, request)
            val unavailable = result.exceptionOrNull() is AiError.ServiceUnavailable
            if (unavailable && fallbackModel != null) call(fallbackModel, apiKey, request) else result
        }
    }

    private fun call(model: String, apiKey: String, request: GenerateContentRequest): Result<Explanation> =
        try {
            Result.success(send(model, apiKey, request))
        } catch (e: AiError) {
            Result.failure(e)
        } catch (e: SocketTimeoutException) {
            Result.failure(AiError.Timeout(e))
        } catch (e: IOException) {
            // DNS failures, refused connections, dropped connections...: treat as connectivity problems.
            Result.failure(AiError.NoInternet(e))
        } catch (e: SerializationException) {
            // Also covers an answer that doesn't follow the explanation schema.
            Result.failure(AiError.Unknown("Unexpected Gemini response", e))
        }

    private fun send(model: String, apiKey: String, body: GenerateContentRequest): Explanation {
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(json.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) throw GeminiErrorParser.parse(response.code, responseBody)
            val answer = extractText(json.decodeFromString<GenerateContentResponse>(responseBody))
            return json.decodeFromString<Explanation>(answer).normalized()
        }
    }

    private fun extractText(response: GenerateContentResponse): String {
        response.promptFeedback?.blockReason?.let { throw AiError.ContentBlocked(it) }
        val candidate = response.candidates.firstOrNull() ?: throw AiError.Unknown("Gemini returned no candidates")
        val text = candidate.content?.parts.orEmpty()
            .filter { it.thought != true }
            .mapNotNull { it.text }
            .joinToString(separator = "")
            .trim()
        if (text.isEmpty()) {
            // An empty answer with a finish reason like SAFETY means the output was filtered.
            throw candidate.finishReason?.takeIf { it != "STOP" }?.let { AiError.ContentBlocked(it) }
                ?: AiError.Unknown("Gemini returned no text")
        }
        return text
    }

    // Models sometimes return "" instead of null for an empty caveat.
    private fun Explanation.normalized(): Explanation = copy(
        mainIdea = mainIdea.trim(),
        simpleExplanation = simpleExplanation.trim(),
        analogy = analogy.trim(),
        caveat = caveat?.trim()?.takeIf { it.isNotEmpty() },
    )

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"

        // Alias that Google keeps pointing at the current Flash model, so a sideloaded APK
        // keeps working when older model versions are retired.
        const val DEFAULT_MODEL = "gemini-flash-latest"

        // Lighter model that stays available when Flash is saturated ("high demand" 503s).
        const val DEFAULT_FALLBACK_MODEL = "gemini-flash-lite-latest"

        private const val TEMPERATURE = 0.4
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
