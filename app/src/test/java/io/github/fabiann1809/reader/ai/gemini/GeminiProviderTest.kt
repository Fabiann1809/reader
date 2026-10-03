package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GeminiProviderTest {

    private val server = MockWebServer()
    private val keyStore = FakeApiKeyStore(initialKey = "test-key-123")
    private lateinit var provider: GeminiProvider

    @Before
    fun setUp() {
        server.start()
        provider = GeminiProvider(
            apiKeyStore = keyStore,
            systemInstruction = "Explain simply.",
            httpClient = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build(),
            baseUrl = server.url("/").toString().trimEnd('/'),
            model = "test-model",
            fallbackModel = "fallback-model",
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    /** A successful response whose answer is [texts] (the model may split it into several parts). */
    private fun enqueueAnswer(vararg texts: String) {
        val response = buildJsonObject {
            putJsonArray("candidates") {
                addJsonObject {
                    putJsonObject("content") {
                        putJsonArray("parts") { texts.forEach { text -> addJsonObject { put("text", text) } } }
                    }
                }
            }
        }
        enqueue(200, response.toString())
    }

    private suspend inline fun <reified T : AiError> assertFailsWith() {
        val error = provider.explain("text").exceptionOrNull()
        assertNotNull(error)
        assertTrue("Expected ${T::class.simpleName} but was $error", error is T)
    }

    @Test
    fun sendsKeyInHeaderAndPromptInBody() = runTest {
        enqueueAnswer(EXPLANATION_JSON)

        provider.explain("A hard paragraph")

        val request = server.takeRequest()
        assertEquals("/v1beta/models/test-model:generateContent", request.url.encodedPath)
        assertEquals("test-key-123", request.headers["x-goog-api-key"])
        // The key must never travel in the URL, where it could end up in server logs.
        assertTrue(request.url.query == null)
        val body = request.body!!.utf8()
        assertTrue(body.contains("A hard paragraph"))
        assertTrue(body.contains("Explain simply."))
        // The answer is requested as JSON following the explanation schema.
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(body.contains("\"responseSchema\""))
    }

    @Test
    fun decodesTheExplanationJoiningPartsAndSkippingThoughts() = runTest {
        val response = buildJsonObject {
            putJsonArray("candidates") {
                addJsonObject {
                    putJsonObject("content") {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", "internal reasoning")
                                put("thought", true)
                            }
                            addJsonObject { put("text", EXPLANATION_JSON.take(40)) }
                            addJsonObject { put("text", EXPLANATION_JSON.drop(40)) }
                        }
                    }
                }
            }
        }
        enqueue(200, response.toString())

        val explanation = provider.explain("text").getOrThrow()

        assertEquals("La luz es energía.", explanation.mainIdea)
        assertEquals("La luz lleva energía.", explanation.simpleExplanation)
        assertEquals("Como el calor del sol.", explanation.analogy)
        assertEquals(listOf(KeyTerm("Fotón", "Partícula de luz.")), explanation.keyTerms)
        assertNull(explanation.caveat)
    }

    @Test
    fun blankCaveatBecomesNull() = runTest {
        enqueueAnswer(EXPLANATION_JSON.replace("\"caveat\":null", "\"caveat\":\"  \""))

        assertNull(provider.explain("text").getOrThrow().caveat)
    }

    @Test
    fun answerOutsideTheSchemaMapsToUnknown() = runTest {
        enqueueAnswer("Idea central: la luz es energía.")

        assertFailsWith<AiError.Unknown>()
    }

    @Test
    fun missingKeyFailsWithoutCallingTheApi() = runTest {
        keyStore.clearApiKey()

        assertFailsWith<AiError.MissingApiKey>()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun invalidKeyResponseMapsToInvalidApiKey() = runTest {
        // Real response captured from the Gemini API with a bogus key.
        enqueue(
            400,
            """
            {"error":{"code":400,"message":"API key not valid. Please pass a valid API key.",
              "status":"INVALID_ARGUMENT","details":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo",
              "reason":"API_KEY_INVALID","domain":"googleapis.com"}]}}
            """.trimIndent(),
        )

        assertFailsWith<AiError.InvalidApiKey>()
    }

    @Test
    fun permissionDeniedMapsToInvalidApiKey() = runTest {
        enqueue(403, """{"error":{"code":403,"status":"PERMISSION_DENIED"}}""")

        assertFailsWith<AiError.InvalidApiKey>()
    }

    @Test
    fun dailyQuotaMapsToQuotaExhausted() = runTest {
        enqueue(
            429,
            """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
              {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
                {"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier"}]}]}}
            """.trimIndent(),
        )

        assertFailsWith<AiError.QuotaExhausted>()
    }

    @Test
    fun perMinuteLimitMapsToRateLimited() = runTest {
        enqueue(
            429,
            """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
              {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
                {"quotaId":"GenerateRequestsPerMinutePerProjectPerModel-FreeTier"}]}]}}
            """.trimIndent(),
        )

        assertFailsWith<AiError.RateLimited>()
    }

    @Test
    fun serverErrorMapsToServiceUnavailable() = runTest {
        // Both the main and the fallback model are down.
        enqueue(503, """{"error":{"code":503,"status":"UNAVAILABLE"}}""")
        enqueue(503, """{"error":{"code":503,"status":"UNAVAILABLE"}}""")

        assertFailsWith<AiError.ServiceUnavailable>()
    }

    @Test
    fun overloadedModelFallsBackToFallbackModel() = runTest {
        enqueue(503, """{"error":{"code":503,"status":"UNAVAILABLE"}}""")
        enqueueAnswer(EXPLANATION_JSON)

        val result = provider.explain("text")

        assertEquals("La luz es energía.", result.getOrNull()?.mainIdea)
        assertEquals("/v1beta/models/test-model:generateContent", server.takeRequest().url.encodedPath)
        assertEquals("/v1beta/models/fallback-model:generateContent", server.takeRequest().url.encodedPath)
    }

    @Test
    fun clientErrorsDoNotTriggerFallback() = runTest {
        enqueue(400, """{"error":{"code":400,"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""")

        assertFailsWith<AiError.InvalidApiKey>()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun unreachableServerMapsToNoInternet() = runTest {
        server.close()

        assertFailsWith<AiError.NoInternet>()
    }

    @Test
    fun slowResponseMapsToTimeout() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body("""{"candidates":[{"content":{"parts":[{"text":"late"}]}}]}""")
                .headersDelay(3, TimeUnit.SECONDS)
                .build(),
        )

        assertFailsWith<AiError.Timeout>()
    }

    @Test
    fun blockedPromptMapsToContentBlocked() = runTest {
        enqueue(200, """{"promptFeedback":{"blockReason":"SAFETY"}}""")

        assertFailsWith<AiError.ContentBlocked>()
    }

    @Test
    fun sendsTheAudioInlineAndReturnsTheTranscript() = runTest {
        enqueueAnswer("Una idea ", "sobre el capítulo.")

        val transcript = provider.transcribe(byteArrayOf(1, 2, 3), "audio/mp4").getOrThrow()

        assertEquals("Una idea sobre el capítulo.", transcript)
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body, body.contains("\"inlineData\":{\"mimeType\":\"audio/mp4\",\"data\":\"AQID\"}"))
        assertTrue(body, body.contains("Transcribe palabra por palabra"))
    }

    @Test
    fun silenceIsAnEmptyTranscriptNotAnError() = runTest {
        enqueue(200, """{"candidates":[{"content":{"parts":[{"text":""}]},"finishReason":"STOP"}]}""")

        assertEquals("", provider.transcribe(byteArrayOf(1), "audio/mp4").getOrThrow())
    }

    @Test
    fun anEmptyExplanationIsStillAnError() = runTest {
        enqueue(200, """{"candidates":[{"content":{"parts":[{"text":""}]},"finishReason":"STOP"}]}""")

        assertFailsWith<AiError.Unknown>()
    }

    @Test
    fun proposesACardFromTheTextWithItsSchema() = runTest {
        enqueueAnswer("""{"front":" ¿Qué mide la entropía? ","back":"El desorden de un sistema."}""")

        val draft = provider.makeFlashcard("La entropía es una medida del desorden.").getOrThrow()

        assertEquals("¿Qué mide la entropía?", draft.front)
        assertEquals("El desorden de un sistema.", draft.back)
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body, body.contains("fichas de repaso"))
        assertTrue(body, body.contains("\"front\""))
    }

    @Test
    fun analysesAnInterpretationLeavingEmptyPartsOut() = runTest {
        enqueueAnswer("""{"understood":"Captas el desorden.","incomplete":"  ","confused":null}""")

        val analysis = provider.analyzeInterpretation("La entropía mide el desorden.", "Habla del orden").getOrThrow()

        assertEquals("Captas el desorden.", analysis.understood)
        assertNull(analysis.incomplete)
        assertNull(analysis.confused)
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body, body.contains("Lo que entendió la persona"))
        assertTrue(body, body.contains("Habla del orden"))
    }

    @Test
    fun malformedJsonMapsToUnknown() = runTest {
        enqueue(200, "not json")

        assertFailsWith<AiError.Unknown>()
    }

    private companion object {
        val EXPLANATION_JSON = """
            {"mainIdea":"La luz es energía.","simpleExplanation":"La luz lleva energía.",
            "analogy":"Como el calor del sol.","keyTerms":[{"term":"Fotón","definition":"Partícula de luz."}],
            "caveat":null}
        """.trimIndent().replace("\n", "")
    }
}
