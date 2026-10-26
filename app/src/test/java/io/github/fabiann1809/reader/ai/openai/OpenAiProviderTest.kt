package io.github.fabiann1809.reader.ai.openai

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.ExplanationChange
import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import io.github.fabiann1809.reader.testing.testExplanation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenAiProviderTest {

    private val server = MockWebServer()
    private val keyStore = FakeApiKeyStore(initialKey = "sk-test-123")

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    // The mock server speaks plain http, so the https rule is turned off, except in the test that checks it.
    private fun provider(
        strict: Boolean = true,
        temperature: Boolean = false,
        model: String = "gpt-test",
        base: String = baseUrl(),
        requireHttps: Boolean = false,
    ) =
        OpenAiProvider(
            apiKeyStore = keyStore,
            httpClient = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build(),
            baseUrl = base,
            model = model,
            strictSchema = strict,
            sendTemperature = temperature,
            requireHttps = requireHttps,
        )

    private fun baseUrl() = server.url("/v1").toString()

    private fun answer(content: String, finishReason: String = "stop"): String {
        val message = JsonObject(mapOf("role" to kotlinx.serialization.json.JsonPrimitive("assistant"), "content" to kotlinx.serialization.json.JsonPrimitive(content)))
        val choice = JsonObject(mapOf("message" to message, "finish_reason" to kotlinx.serialization.json.JsonPrimitive(finishReason)))
        return JsonObject(mapOf("choices" to kotlinx.serialization.json.JsonArray(listOf(choice)))).toString()
    }

    private fun enqueue(code: Int, body: String) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    private val explanationJson = """{"mainIdea":" Idea ","simpleExplanation":"Simple","analogy":"Como...","keyTerms":[{"term":"A","definition":"B"}],"caveat":""}"""

    @Test
    fun anAddressThatIsNotHttpsIsRejectedBeforeAnyRequest() = runTest {
        val result = provider(base = "http://example.com/v1", requireHttps = true).explain("texto")

        assertTrue(result.exceptionOrNull() is AiError.NotConfigured)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun aMissingModelOrKeyFailsBeforeAnyRequest() = runTest {
        assertTrue(provider(model = " ").explain("t").exceptionOrNull() is AiError.NotConfigured)

        keyStore.clearApiKey()
        assertTrue(provider().explain("t").exceptionOrNull() is AiError.MissingApiKey)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun sendsTheKeyInTheHeaderAndAskedForStrictStructuredOutput() = runTest {
        enqueue(200, answer(explanationJson))

        provider(strict = true).explain("Un párrafo difícil")

        val request = server.takeRequest()
        assertEquals("/v1/chat/completions", request.url.encodedPath)
        assertEquals("Bearer sk-test-123", request.headers["Authorization"])
        assertNull(request.url.query)
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("gpt-test", body["model"]!!.jsonPrimitive.content)
        assertFalse("temperature" in body)
        val format = body["response_format"]!!.jsonObject
        assertEquals("json_schema", format["type"]!!.jsonPrimitive.content)
        val schema = format["json_schema"]!!.jsonObject
        assertEquals("true", schema["strict"]!!.jsonPrimitive.content)
        assertEquals("false", schema["schema"]!!.jsonObject["additionalProperties"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray
        assertEquals("system", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("Un párrafo difícil", messages[1].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun aCompatibleServiceGetsAJsonObjectWithTheSchemaInTheInstruction() = runTest {
        enqueue(200, answer(explanationJson))

        provider(strict = false, temperature = true).explain("texto")

        val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals("json_object", body["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("0.4", body["temperature"]!!.jsonPrimitive.content)
        val system = body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonPrimitive.content
        assertTrue(system.contains("esquema JSON"))
        assertTrue(system.contains("mainIdea"))
    }

    @Test
    fun decodesTheExplanationAndTidiesIt() = runTest {
        enqueue(200, answer(explanationJson))

        val explanation = provider().explain("texto").getOrThrow()

        assertEquals("Idea", explanation.mainIdea)
        assertEquals("A", explanation.keyTerms.single().term)
        assertNull(explanation.caveat)
    }

    @Test
    fun readsAnAnswerWrappedInACodeBlock() = runTest {
        enqueue(200, answer("```json\n$explanationJson\n```"))

        assertEquals("Simple", provider(strict = false).explain("texto").getOrThrow().simpleExplanation)
    }

    @Test
    fun refineSendsThePreviousExplanationAndWhatIsAsked() = runTest {
        enqueue(200, answer(explanationJson))

        provider().refine("Mi texto", testExplanation(), ExplanationChange.SIMPLER)

        val user = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject["messages"]!!.jsonArray[1]
            .jsonObject["content"]!!.jsonPrimitive.content
        assertTrue(user.contains("Mi texto"))
        assertTrue(user.contains(testExplanation().analogy))
        assertTrue(user.contains("más simples"))
    }

    @Test
    fun theOtherThreeFeaturesDecodeToo() = runTest {
        enqueue(200, answer("""{"front":" ¿Qué? ","back":"Eso"}"""))
        enqueue(200, answer("""{"understood":"Bien","incomplete":"","confused":null}"""))
        enqueue(200, answer("""{"questions":[{"question":"¿A?","options":["1","2","3","4"],"correctIndex":2,"explanation":"Porque","topic":"Tema"}]}"""))
        val provider = provider()

        assertEquals("¿Qué?", provider.makeFlashcard("t").getOrThrow().front)
        val analysis = provider.analyzeInterpretation("t", "mi idea").getOrThrow()
        assertEquals("Bien", analysis.understood)
        assertNull(analysis.incomplete)
        assertEquals(2, provider.generateQuiz("t", 1).getOrThrow().questions.single().correctIndex)
    }

    @Test
    fun translatesHttpErrors() = runTest {
        suspend fun failure(code: Int, body: String): Throwable? {
            enqueue(code, body)
            return provider().explain("t").exceptionOrNull()
        }

        assertTrue(failure(401, """{"error":{"message":"bad key"}}""") is AiError.InvalidApiKey)
        assertTrue(failure(429, """{"error":{"code":"insufficient_quota","type":"insufficient_quota"}}""") is AiError.QuotaExhausted)
        assertTrue(failure(429, """{"error":{"type":"requests"}}""") is AiError.RateLimited)
        assertTrue(failure(404, """{"error":{"code":"model_not_found"}}""") is AiError.ModelNotFound)
        assertTrue(failure(503, "<html>down</html>") is AiError.ServiceUnavailable)
        assertTrue(failure(400, "{}") is AiError.Unknown)
    }

    @Test
    fun aRefusalOrAFilteredAnswerIsBlockedContent() = runTest {
        enqueue(200, """{"choices":[{"message":{"content":null,"refusal":"No puedo"},"finish_reason":"stop"}]}""")
        assertTrue(provider().explain("t").exceptionOrNull() is AiError.ContentBlocked)

        enqueue(200, answer("", finishReason = "content_filter"))
        assertTrue(provider().explain("t").exceptionOrNull() is AiError.ContentBlocked)
    }

    @Test
    fun anAnswerThatIsNotTheExpectedShapeIsUnknown() = runTest {
        enqueue(200, answer("no es json"))
        assertTrue(provider().explain("t").exceptionOrNull() is AiError.Unknown)

        enqueue(200, "{}")
        assertTrue(provider().explain("t").exceptionOrNull() is AiError.Unknown)
    }

    @Test
    fun theKeyNeverAppearsInAnError() = runTest {
        enqueue(401, """{"error":{"message":"Incorrect API key provided: sk-test-123"}}""")

        val error = provider().explain("t").exceptionOrNull()

        assertFalse(error?.message.orEmpty().contains("sk-test-123"))
    }

    private fun transcriber(transcribes: Boolean = true) = OpenAiProvider(
        apiKeyStore = keyStore,
        httpClient = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build(),
        baseUrl = baseUrl(),
        model = "gpt-test",
        strictSchema = true,
        sendTemperature = false,
        requireHttps = false,
        transcribes = transcribes,
    )

    @Test
    fun transcribesTheAudioWithTheKeyInTheHeader() = runTest {
        enqueue(200, """{"text":"  Una idea del capítulo  "}""")

        val text = transcriber().transcribe(byteArrayOf(1, 2, 3, 4), "audio/mp4").getOrThrow()

        assertEquals("Una idea del capítulo", text)
        val request = server.takeRequest()
        assertEquals("/v1/audio/transcriptions", request.url.encodedPath)
        assertEquals("Bearer sk-test-123", request.headers["Authorization"])
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("whisper-1"))
        assertTrue(body.contains("filename=\"nota.m4a\""))
        assertTrue(body.contains("audio/mp4"))
    }

    @Test
    fun silenceIsAnEmptyTranscriptNotAnError() = runTest {
        enqueue(200, """{"text":""}""")

        assertEquals("", transcriber().transcribe(byteArrayOf(1), "audio/mp4").getOrThrow())
    }

    @Test
    fun aCompatibleServiceDoesNotTranscribe() = runTest {
        val result = transcriber(transcribes = false).transcribe(byteArrayOf(1), "audio/mp4")

        assertTrue(result.exceptionOrNull() is AiError.TranscriptionNotSupported)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun aTranscriptionErrorIsTranslatedLikeAnyOther() = runTest {
        enqueue(401, """{"error":{"message":"bad key"}}""")

        assertTrue(transcriber().transcribe(byteArrayOf(1), "audio/mp4").exceptionOrNull() is AiError.InvalidApiKey)
    }
}
