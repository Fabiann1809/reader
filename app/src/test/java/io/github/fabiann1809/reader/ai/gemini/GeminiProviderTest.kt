package io.github.fabiann1809.reader.ai.gemini

import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
            httpClient = OkHttpClient(),
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
    fun failsWithoutStoredKeyAndDoesNotCallTheApi() = runTest {
        keyStore.clearApiKey()

        val result = provider.explain("text")

        assertTrue(result.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun failsOnHttpError() = runTest {
        enqueue(400, """{"error":{"code":400,"status":"INVALID_ARGUMENT"}}""")

        assertTrue(provider.explain("text").isFailure)
    }

    @Test
    fun failsWhenPromptIsBlocked() = runTest {
        enqueue(200, """{"promptFeedback":{"blockReason":"SAFETY"}}""")

        assertTrue(provider.explain("text").isFailure)
    }

    @Test
    fun failsOnMalformedJson() = runTest {
        enqueue(200, "not json")

        assertTrue(provider.explain("text").isFailure)
    }
}
