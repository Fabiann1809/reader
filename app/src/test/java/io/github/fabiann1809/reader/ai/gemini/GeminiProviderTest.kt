package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private suspend inline fun <reified T : AiError> assertFailsWith() {
        val error = provider.explain("text").exceptionOrNull()
        assertNotNull(error)
        assertTrue("Expected ${T::class.simpleName} but was $error", error is T)
    }

    @Test
    fun sendsKeyInHeaderAndPromptInBody() = runTest {
        enqueue(200, """{"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}""")

        provider.explain("A hard paragraph")

        val request = server.takeRequest()
        assertEquals("/v1beta/models/test-model:generateContent", request.url.encodedPath)
        assertEquals("test-key-123", request.headers["x-goog-api-key"])
        // The key must never travel in the URL, where it could end up in server logs.
        assertTrue(request.url.query == null)
        val body = request.body!!.utf8()
        assertTrue(body.contains("A hard paragraph"))
        assertTrue(body.contains("Explain simply."))
    }

    @Test
    fun returnsTextJoiningPartsAndSkippingThoughts() = runTest {
        enqueue(
            200,
            """
            {"candidates":[{"content":{"parts":[
              {"text":"internal reasoning","thought":true},
              {"text":"Idea central: "},
              {"text":"la luz es energía."}
            ]}}]}
            """.trimIndent(),
        )

        val result = provider.explain("text")

        assertEquals("Idea central: la luz es energía.", result.getOrNull())
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
        enqueue(503, """{"error":{"code":503,"status":"UNAVAILABLE"}}""")

        assertFailsWith<AiError.ServiceUnavailable>()
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
    fun malformedJsonMapsToUnknown() = runTest {
        enqueue(200, "not json")

        assertFailsWith<AiError.Unknown>()
    }
}
