package io.github.fabiann1809.reader.ai

/**
 * Abstraction over any AI model provider. Every model call in the app goes through this
 * interface, so the provider can be swapped without touching the UI.
 *
 * Implementations must never log or expose the API key.
 */
interface AiProvider {

    /**
     * Explains [text] using the Feynman method.
     * Returns the explanation split into blocks, or a failure (an [AiError]) describing why the call did not succeed.
     */
    suspend fun explain(text: String): Result<Explanation>

    /**
     * Writes down what is said in [audio], encoded as [mimeType] (T13.2). An empty text means no
     * words were heard; failures are [AiError]s, like [explain]'s.
     */
    suspend fun transcribe(audio: ByteArray, mimeType: String): Result<String>
}
