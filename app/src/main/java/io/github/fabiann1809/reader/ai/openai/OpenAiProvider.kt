package io.github.fabiann1809.reader.ai.openai

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
import kotlinx.serialization.json.add
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
 * [AiProvider] for OpenAI and for any service that speaks its chat API (`/chat/completions`).
 *
 * The key is read from [apiKeyStore] on each call and sent only in the `Authorization` header.
 * Nothing here logs requests, headers or the key. With [strictSchema] (OpenAI itself) the answer is
 * asked as structured output; otherwise as a JSON object, with the schema written in the
 * instruction, which most compatible services accept. Every failure is an [AiError].
 */
class OpenAiProvider(
    private val apiKeyStore: ApiKeyStore,
    private val httpClient: OkHttpClient,
    private val baseUrl: String,
    private val model: String,
    private val strictSchema: Boolean,
    // Some OpenAI models only accept their default temperature, so it is only sent where it is allowed.
    private val sendTemperature: Boolean,
    // The key travels in every request, so the address must be encrypted; only tests turn this off.
    private val requireHttps: Boolean = true,
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

    override suspend fun transcribe(audio: ByteArray, mimeType: String): Result<String> =
        Result.failure(AiError.Unknown("Transcription is not available with this provider yet"))

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
        schemaName: String,
        geminiSchema: JsonObject,
        parse: (String) -> T,
    ): Result<T> {
        if (model.isBlank() || (requireHttps && !baseUrl.startsWith("https://"))) return Result.failure(AiError.NotConfigured())
        val apiKey = apiKeyStore.getApiKey() ?: return Result.failure(AiError.MissingApiKey())
        val schema = geminiSchema.toJsonSchema(strict = strictSchema)
        val body = requestBody(instruction, message, schemaName, schema)
        return withContext(Dispatchers.IO) { runAiCall { parse(AiResponses.unwrapJson(send(apiKey, body))) } }
    }

    private fun requestBody(instruction: String, message: String, schemaName: String, schema: JsonObject): String =
        buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    // Without structured outputs the shape has to be spelled out for the model.
                    put("content", if (strictSchema) instruction else "$instruction\n\n${schemaInstruction(schema)}")
                }
                addJsonObject {
                    put("role", "user")
                    put("content", message)
                }
            }
            if (sendTemperature) put("temperature", TEMPERATURE)
            putJsonObject("response_format") {
                if (strictSchema) {
                    put("type", "json_schema")
                    putJsonObject("json_schema") {
                        put("name", schemaName)
                        put("strict", true)
                        put("schema", schema)
                    }
                } else {
                    put("type", "json_object")
                }
            }
        }.toString()

    private fun schemaInstruction(schema: JsonObject) =
        "Responde únicamente con un objeto JSON válido, sin texto antes ni después, que cumpla este esquema JSON:\n$schema"

    /** The text of the model's answer, or an [AiError] saying why there is none. */
    private fun send(apiKey: String, body: String): String {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) throw OpenAiErrorParser.parse(response.code, responseBody)
            return answerText(responseBody)
        }
    }

    private fun answerText(responseBody: String): String {
        val choice = json.parseToJsonElement(responseBody).jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw AiError.Unknown("The response has no choices")
        val message = choice["message"]?.jsonObject
        (message?.get("refusal") as? JsonPrimitive)?.takeIf { it.isString }?.let { throw AiError.ContentBlocked("refusal") }
        val text = (message?.get("content") as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty().trim()
        if (text.isEmpty()) {
            val reason = (choice["finish_reason"] as? JsonPrimitive)?.content
            if (reason == "content_filter") throw AiError.ContentBlocked(reason)
            throw AiError.Unknown("The response has no text")
        }
        return text
    }

    companion object {
        const val OPENAI_BASE_URL = "https://api.openai.com/v1"
        private const val TEMPERATURE = 0.4
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
