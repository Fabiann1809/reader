package io.github.fabiann1809.reader.ai.anthropic

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.ExplanationChange
import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import io.github.fabiann1809.reader.testing.testExplanation
import kotlinx.coroutines.test.runTest
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

class AnthropicProviderTest {

    private val server = MockWebServer()
    private val keyStore = FakeApiKeyStore(initialKey = "sk-ant-test")

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun provider(model: String = "claude-test") = AnthropicProvider(
        apiKeyStore = keyStore,
        httpClient = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build(),
        model = model,
        baseUrl = server.url("/").toString().trimEnd('/'),
    )

    private fun enqueue(code: Int, body: String) = server.enqueue(MockResponse.Builder().code(code).body(body).build())

    /** A Messages API answer in which the model called the tool [name] with [input]. */
    private fun toolAnswer(input: String, name: String = "explanation", stopReason: String = "tool_use") =
        """{"content":[{"type":"text","text":"Voy a registrarlo."},{"type":"tool_use","id":"t1","name":"$name","input":$input}],"stop_reason":"$stopReason"}"""

    private val explanationInput = """{"mainIdea":" Idea ","simpleExplanation":"Simple","analogy":"Como...","keyTerms":[{"term":"A","definition":"B"}],"caveat":""}"""

    @Test
    fun sendsTheKeyInTheHeaderAndForcesTheTool() = runTest {
        enqueue(200, toolAnswer(explanationInput))

        provider().explain("Un párrafo difícil")

        val request = server.takeRequest()
        assertEquals("/v1/messages", request.url.encodedPath)
        assertEquals("sk-ant-test", request.headers["x-api-key"])
        assertEquals("2023-06-01", request.headers["anthropic-version"])
        assertNull(request.url.query)
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("claude-test", body["model"]!!.jsonPrimitive.content)
        assertTrue(body["max_tokens"]!!.jsonPrimitive.content.toInt() > 0)
        assertEquals("Un párrafo difícil", body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonPrimitive.content)
        val tool = body["tools"]!!.jsonArray.single().jsonObject
        assertEquals("explanation", tool["name"]!!.jsonPrimitive.content)
        assertEquals("object", tool["input_schema"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("tool", body["tool_choice"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("explanation", body["tool_choice"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun decodesTheToolInputAndTidiesIt() = runTest {
        enqueue(200, toolAnswer(explanationInput))

        val explanation = provider().explain("texto").getOrThrow()

        assertEquals("Idea", explanation.mainIdea)
        assertEquals("A", explanation.keyTerms.single().term)
        assertNull(explanation.caveat)
    }

    @Test
    fun refineSendsThePreviousExplanationAndWhatIsAsked() = runTest {
        enqueue(200, toolAnswer(explanationInput))

        provider().refine("Mi texto", testExplanation(), ExplanationChange.ANOTHER_EXAMPLE)

        val user = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject["messages"]!!.jsonArray[0]
            .jsonObject["content"]!!.jsonPrimitive.content
        assertTrue(user.contains("Mi texto"))
        assertTrue(user.contains(testExplanation().analogy))
        assertTrue(user.contains("analogía y un ejemplo distintos"))
    }

    @Test
    fun theOtherThreeFeaturesDecodeToo() = runTest {
        enqueue(200, toolAnswer("""{"front":" ¿Qué? ","back":"Eso"}""", name = "flashcard"))
        enqueue(200, toolAnswer("""{"understood":"Bien","incomplete":""}""", name = "interpretation"))
        enqueue(
            200,
            toolAnswer(
                """{"questions":[{"question":"¿A?","options":["1","2","3","4"],"correctIndex":1,"explanation":"Porque","topic":"Tema"}]}""",
                name = "quiz",
            ),
        )
        val provider = provider()

        assertEquals("¿Qué?", provider.makeFlashcard("t").getOrThrow().front)
        val analysis = provider.analyzeInterpretation("t", "mi idea").getOrThrow()
        assertEquals("Bien", analysis.understood)
        assertNull(analysis.incomplete)
        assertEquals(1, provider.generateQuiz("t", 1).getOrThrow().questions.single().correctIndex)
    }

    @Test
    fun translatesHttpErrors() = runTest {
        suspend fun failure(code: Int, body: String): Throwable? {
            enqueue(code, body)
            return provider().explain("t").exceptionOrNull()
        }

        assertTrue(failure(401, """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""") is AiError.InvalidApiKey)
        assertTrue(failure(404, """{"type":"error","error":{"type":"not_found_error","message":"model: nope"}}""") is AiError.ModelNotFound)
        assertTrue(failure(429, """{"type":"error","error":{"type":"rate_limit_error"}}""") is AiError.RateLimited)
        assertTrue(failure(529, """{"type":"error","error":{"type":"overloaded_error"}}""") is AiError.ServiceUnavailable)
        assertTrue(
            failure(400, """{"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low"}}""")
                is AiError.QuotaExhausted,
        )
        assertTrue(failure(400, """{"type":"error","error":{"type":"invalid_request_error","message":"bad"}}""") is AiError.Unknown)
    }

    @Test
    fun aRefusalIsBlockedContent() = runTest {
        enqueue(200, """{"content":[{"type":"text","text":"No puedo."}],"stop_reason":"refusal"}""")

        assertTrue(provider().explain("t").exceptionOrNull() is AiError.ContentBlocked)
    }

    @Test
    fun aMissingToolCallIsUnknown() = runTest {
        enqueue(200, """{"content":[{"type":"text","text":"Hola"}],"stop_reason":"end_turn"}""")

        assertTrue(provider().explain("t").exceptionOrNull() is AiError.Unknown)
    }

    @Test
    fun aMissingKeyOrModelFailsBeforeAnyRequest() = runTest {
        assertTrue(provider(model = " ").explain("t").exceptionOrNull() is AiError.NotConfigured)

        keyStore.clearApiKey()
        assertTrue(provider().explain("t").exceptionOrNull() is AiError.MissingApiKey)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun claudeDoesNotTranscribeAudio() = runTest {
        val result = provider().transcribe(byteArrayOf(1, 2), "audio/mp4")

        assertTrue(result.exceptionOrNull() is AiError.TranscriptionNotSupported)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun theKeyNeverAppearsInAnError() = runTest {
        enqueue(401, """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key sk-ant-test"}}""")

        val error = provider().explain("t").exceptionOrNull()

        assertFalse(error?.message.orEmpty().contains("sk-ant-test"))
    }
}
