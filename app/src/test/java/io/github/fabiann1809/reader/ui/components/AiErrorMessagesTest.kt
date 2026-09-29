package io.github.fabiann1809.reader.ui.components

import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.AiError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AiErrorMessagesTest {

    @Test
    fun requiredCasesHaveDistinctMessages() {
        val messages = listOf(
            aiErrorMessageRes(AiError.InvalidApiKey()),
            aiErrorMessageRes(AiError.QuotaExhausted()),
            aiErrorMessageRes(AiError.RateLimited()),
            aiErrorMessageRes(AiError.NoInternet(IOException())),
        )

        assertEquals(messages.size, messages.toSet().size)
    }

    @Test
    fun mapsEachErrorToItsMessage() {
        assertEquals(R.string.ai_error_missing_key, aiErrorMessageRes(AiError.MissingApiKey()))
        assertEquals(R.string.ai_error_invalid_key, aiErrorMessageRes(AiError.InvalidApiKey()))
        assertEquals(R.string.ai_error_quota_exhausted, aiErrorMessageRes(AiError.QuotaExhausted()))
        assertEquals(R.string.ai_error_rate_limited, aiErrorMessageRes(AiError.RateLimited()))
        assertEquals(R.string.ai_error_no_internet, aiErrorMessageRes(AiError.NoInternet(IOException())))
        assertEquals(R.string.ai_error_timeout, aiErrorMessageRes(AiError.Timeout(IOException())))
        assertEquals(R.string.ai_error_unavailable, aiErrorMessageRes(AiError.ServiceUnavailable(503)))
        assertEquals(R.string.ai_error_blocked, aiErrorMessageRes(AiError.ContentBlocked("SAFETY")))
    }

    @Test
    fun unexpectedExceptionsGetGenericMessage() {
        assertEquals(R.string.ai_error_unknown, aiErrorMessageRes(IllegalStateException()))
    }
}
