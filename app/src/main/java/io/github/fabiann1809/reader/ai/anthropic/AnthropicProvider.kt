package io.github.fabiann1809.reader.ai.anthropic

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.AiResponses
import io.github.fabiann1809.reader.ai.ExplainerPrompt
import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.ExplanationChange
import io.github.fabiann1809.reader.ai.FlashcardDraft
import io.github.fabiann1809.reader.ai.FlashcardPrompt
import io.github.fabiann1809.reader.ai.InterpretationAnalysis
import io.github.fabiann1809.reader.ai.InterpretationPrompt
import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizPrompt
import io.github.fabiann1809.reader.ai.gemini.ExplanationSchema
import io.github.fabiann1809.reader.ai.gemini.FlashcardSchema
import io.github.fabiann1809.reader.ai.gemini.InterpretationSchema
import io.github.fabiann1809.reader.ai.gemini.QuizSchema
import io.github.fabiann1809.reader.ai.runAiCall
import io.github.fabiann1809.reader.ai.toJsonSchema
import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * [AiProvider] backed by Anthropic's Messages API (Claude).
 *
 * Each answer is asked through a tool the model must call, whose input schema is the shape of the
 * answer: that is how Claude returns structured data. The key is read from [apiKeyStore] on each
 * call and sent only in the `x-api-key` header. Nothing here logs requests, headers or the key.
 * Every failure is an [AiError].
 */
class AnthropicProvider(
    private val apiKeyStore: ApiKeyStore,
    private val httpClient: OkHttpClient,
    private val model: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val explainerInstruction: String = ExplainerPrompt.SYSTEM_INSTRUCTION,
    private val flashcardInstruction: String = FlashcardPrompt.SYSTEM_INSTRUCTION,
    private val interpretationInstruction: String = InterpretationPrompt.SYSTEM_INSTRUCTION,
    private val quizInstruction: String = QuizPrompt.SYSTEM_INSTRUCTION,
) : AiProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun explain(text: String): Result<Explanation> =
        ask(explainerInstruction, text, "explanation", ExplanationSchema, AiResponses::explanation)

    override suspend fun refine(text: String, previous: Explanation, change: ExplanationChange): Result<Explanation> =
        ask(
            explainerInstruction,
            ExplainerPrompt.refineMessage(text, previous, change),
            "explanation",
            ExplanationSchema,
            AiResponses::explanation,
        )

    // Claude does not take audio.
    override suspend fun transcribe(audio: ByteArray, mimeType: String): Result<String> =
        Result.failure(AiError.TranscriptionNotSupported())

    override suspend fun makeFlashcard(text: String): Result<FlashcardDraft> =
        ask(flashcardInstruction, text, "flashcard", FlashcardSchema, AiResponses::flashcard)

    override suspend fun analyzeInterpretation(text: String, interpretation: String): Result<InterpretationAnalysis> =
        ask(
            interpretationInstruction,
            InterpretationPrompt.userMessage(text, interpretation),
            "interpretation",
            InterpretationSchema,
            AiResponses::interpretation,
        )

    override suspend fun generateQuiz(text: String, questionCount: Int): Result<Quiz> =
        ask(quizInstruction, QuizPrompt.userMessage(text, questionCount), "quiz", QuizSchema, AiResponses::quiz)

    private suspend fun <T> ask(
        instruction: String,
        message: String,
        toolName: String,
        geminiSchema: JsonObject,
        parse: (String) -> T,
    ): Result<T> {
        if (model.isBlank()) return Result.failure(AiError.NotConfigured())
        val apiKey = apiKeyStore.getApiKey() ?: return Result.failure(AiError.MissingApiKey())
        val body = requestBody(instruction, message, toolName, geminiSchema.toJsonSchema(strict = false))
        return withContext(Dispatchers.IO) { runAiCall { parse(send(apiKey, body, toolName)) } }
    }

    private fun requestBody(instruction: String, message: String, toolName: String, schema: JsonObject): String =
        buildJsonObject {
            put("model", model)
            put("max_tokens", MAX_TOKENS)
            put("system", instruction)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", message)
                }
            }
            putJsonArray("tools") {
                addJsonObject {
                    put("name", toolName)
                    put("description", "Records the answer in the required format.")
                    put("input_schema", schema)
                }
            }
            // Forcing the tool makes the model answer with its input, which is the data we want.
            putJsonObject("tool_choice") {
                put("type", "tool")
                put("name", toolName)
            }
        }.toString()

    /** The JSON the model gave the tool, or an [AiError] saying why there is none. */
    private fun send(apiKey: String, body: String, toolName: String): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", API_VERSION)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) throw AnthropicErrorParser.parse(response.code, responseBody)
            return toolInput(responseBody, toolName)
        }
    }

    private fun toolInput(responseBody: String, toolName: String): String {
        val answer = json.parseToJsonElement(responseBody).jsonObject
        if ((answer["stop_reason"] as? JsonPrimitive)?.content == "refusal") throw AiError.ContentBlocked("refusal")
        val tool = answer["content"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { (it["type"] as? JsonPrimitive)?.content == "tool_use" && (it["name"] as? JsonPrimitive)?.content == toolName }
            ?: throw AiError.Unknown("The response has no answer")
        return tool["input"]?.jsonObject?.toString() ?: throw AiError.Unknown("The response has no answer")
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"
        private const val API_VERSION = "2023-06-01"

        // Room for the longest answer (a ten question quiz) with plenty to spare.
        private const val MAX_TOKENS = 8_192
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
