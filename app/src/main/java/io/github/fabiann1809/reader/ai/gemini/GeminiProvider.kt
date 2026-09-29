package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProvider
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
 * Every failure is reported as an [AiError].
 */
class GeminiProvider(
    private val apiKeyStore: ApiKeyStore,
    private val systemInstruction: String,
    private val httpClient: OkHttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val model: String = DEFAULT_MODEL,
) : AiProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun explain(text: String): Result<String> {
        val apiKey = apiKeyStore.getApiKey() ?: return Result.failure(AiError.MissingApiKey())

        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            generationConfig = GenerationConfig(temperature = TEMPERATURE),
        )
        return withContext(Dispatchers.IO) {
            try {
                Result.success(send(apiKey, request))
            } catch (e: AiError) {
                Result.failure(e)
            } catch (e: SocketTimeoutException) {
                Result.failure(AiError.Timeout(e))
            } catch (e: IOException) {
                // DNS failures, refused connections, dropped connections...: treat as connectivity problems.
                Result.failure(AiError.NoInternet(e))
            } catch (e: SerializationException) {
                Result.failure(AiError.Unknown("Unexpected Gemini response", e))
            }
        }
    }

    private fun send(apiKey: String, body: GenerateContentRequest): String {
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(json.encodeToString(body).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) throw GeminiErrorParser.parse(response.code, responseBody)
            return extractText(json.decodeFromString<GenerateContentResponse>(responseBody))
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

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"

        // Alias that Google keeps pointing at the current Flash model, so a sideloaded APK
        // keeps working when older model versions are retired.
        const val DEFAULT_MODEL = "gemini-flash-latest"

        private const val TEMPERATURE = 0.4
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
