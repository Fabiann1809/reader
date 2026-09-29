package io.github.fabiann1809.reader.ai

/**
 * Reasons an [AiProvider] call can fail. Providers translate their own errors into these,
 * so the UI can show a specific message without knowing which provider is in use.
 *
 * Messages are for developers only; they must never contain the API key.
 */
sealed class AiError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class MissingApiKey : AiError("No API key configured")

    class InvalidApiKey : AiError("The API key was rejected")

    /** The daily (or longer) quota is used up; retrying today won't help. */
    class QuotaExhausted : AiError("Quota exhausted")

    /** Too many requests in a short time; retrying after a short wait should work. */
    class RateLimited : AiError("Rate limited")

    class NoInternet(cause: Throwable) : AiError("No internet connection", cause)

    class Timeout(cause: Throwable) : AiError("The request timed out", cause)

    class ServiceUnavailable(val httpCode: Int) : AiError("Provider unavailable (HTTP $httpCode)")

    /** The provider refused to process the text (e.g. safety filters). */
    class ContentBlocked(reason: String) : AiError("Content blocked: $reason")

    class Unknown(message: String, cause: Throwable? = null) : AiError(message, cause)
}
