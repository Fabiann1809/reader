package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.FlashcardDraft
import io.github.fabiann1809.reader.ai.FlashcardPrompt
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import io.github.fabiann1809.reader.ai.InterpretationPrompt
import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizPrompt
import io.github.fabiann1809.reader.ai.TranscriptionPrompt
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
import java.util.Base64

/**
 * [AiProvider] backed by Google Gemini (REST `models.generateContent`).
 *
 * The API key is read from [apiKeyStore] on every call and sent only in the
 * `x-goog-api-key` header. Nothing here logs requests, headers or the key.
 * An explanation is requested as JSON with [ExplanationSchema] and decoded into an [Explanation];
 * a transcription sends the audio inline, with [transcriptionInstruction], and gets plain text;
 * a card proposal is JSON with [FlashcardSchema], an interpretation's analysis with [InterpretationSchema]
 * and a quiz with [QuizSchema].
 * Every failure is reported as an [AiError]. When [model] is overloaded (5xx), the request is
 * retried once with [fallbackModel] so the user still gets an answer.
 */
class GeminiProvider(
    private val apiKeyStore: ApiKeyStore,
    private val systemInstruction: String,
    private val transcriptionInstruction: String = TranscriptionPrompt.SYSTEM_INSTRUCTION,
    private val flashcardInstruction: String = FlashcardPrompt.SYSTEM_INSTRUCTION,
    private val interpretationInstruction: String = InterpretationPrompt.SYSTEM_INSTRUCTION,
    private val quizInstruction: String = QuizPrompt.SYSTEM_INSTRUCTION,
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
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            generationConfig = GenerationConfig(
                temperature = TEMPERATURE,
                responseMimeType = "application/json",
                responseSchema = ExplanationSchema,
            ),
        )
        return generate(request) { answer -> json.decodeFromString<Explanation>(answer.requireText()).normalized() }
    }

    override suspend fun transcribe(audio: ByteArray, mimeType: String): Result<String> {
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = transcriptionInstruction))),
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(inlineData = InlineData(mimeType, Base64.getEncoder().encodeToString(audio)))),
                ),
            ),
            // Low temperature: a transcript should repeat what was said, not reword it.
            generationConfig = GenerationConfig(temperature = TRANSCRIPTION_TEMPERATURE),
        )
        // Silence is a valid answer: an empty transcript, not an error.
        return generate(request) { answer -> answer }
    }

    override suspend fun makeFlashcard(text: String): Result<FlashcardDraft> {
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = flashcardInstruction))),
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            generationConfig = GenerationConfig(
                temperature = TEMPERATURE,
                responseMimeType = "application/json",
                responseSchema = FlashcardSchema,
            ),
        )
        return generate(request) { answer ->
            json.decodeFromString<FlashcardDraft>(answer.requireText()).let { FlashcardDraft(it.front.trim(), it.back.trim()) }
        }
    }

    override suspend fun analyzeInterpretation(text: String, interpretation: String): Result<InterpretationAnalysis> {
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = interpretationInstruction))),
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = InterpretationPrompt.userMessage(text, interpretation)))),
            ),
            generationConfig = GenerationConfig(
                temperature = TEMPERATURE,
                responseMimeType = "application/json",
                responseSchema = InterpretationSchema,
            ),
        )
        return generate(request) { answer ->
            val analysis = json.decodeFromString<InterpretationAnalysis>(answer.requireText())
            // Models sometimes write "" instead of leaving a part out.
            InterpretationAnalysis(
                understood = analysis.understood.orBlankAsNull(),
                incomplete = analysis.incomplete.orBlankAsNull(),
                confused = analysis.confused.orBlankAsNull(),
            )
        }
    }

    override suspend fun generateQuiz(text: String, questionCount: Int): Result<Quiz> {
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = quizInstruction))),
            contents = listOf(Content(role = "user", parts = listOf(Part(text = QuizPrompt.userMessage(text, questionCount))))),
            generationConfig = GenerationConfig(
                temperature = TEMPERATURE,
                responseMimeType = "application/json",
                responseSchema = QuizSchema,
            ),
        )
        return generate(request) { answer ->
            val quiz = json.decodeFromString<Quiz>(answer.requireText())
            Quiz(
                quiz.questions.map { question ->
                    question.copy(
                        question = question.question.trim(),
                        options = question.options.map { it.trim() },
                        explanation = question.explanation.trim(),
                        topic = question.topic.trim(),
                    )
                },
            )
        }
    }

    private fun String?.orBlankAsNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** Sends [request], falling back to [fallbackModel] when [model] is overloaded, and [parse]s the answer. */
    private suspend fun <T> generate(request: GenerateContentRequest, parse: (String) -> T): Result<T> {
        val apiKey = apiKeyStore.getApiKey() ?: return Result.failure(AiError.MissingApiKey())
        return withContext(Dispatchers.IO) {
            val result = call(model, apiKey, request, parse)
            val unavailable = result.exceptionOrNull() is AiError.ServiceUnavailable
            if (unavailable && fallbackModel != null) call(fallbackModel, apiKey, request, parse) else result
        }
    }

    private fun <T> call(model: String, apiKey: String, request: GenerateContentRequest, parse: (String) -> T): Result<T> =
        try {
            Result.success(parse(send(model, apiKey, request)))
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

    /** The model's answer as text; empty only when the model ended normally without saying anything. */
    private fun send(model: String, apiKey: String, body: GenerateContentRequest): String {
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
        // An empty answer with a finish reason like SAFETY means the output was filtered.
        if (text.isEmpty()) candidate.finishReason?.takeIf { it != "STOP" }?.let { throw AiError.ContentBlocked(it) }
        return text
    }

    private fun String.requireText(): String = ifEmpty { throw AiError.Unknown("Gemini returned no text") }

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
        private const val TRANSCRIPTION_TEMPERATURE = 0.0
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
