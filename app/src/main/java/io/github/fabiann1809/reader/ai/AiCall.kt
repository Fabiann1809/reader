package io.github.fabiann1809.reader.ai

import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException

/** Runs a network call and reports whatever goes wrong as an [AiError], so providers all fail the same way. */
internal inline fun <T> runAiCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: AiError) {
        Result.failure(e)
    } catch (e: SocketTimeoutException) {
        Result.failure(AiError.Timeout(e))
    } catch (e: IOException) {
        // DNS failures, refused connections, dropped connections...: treat as connectivity problems.
        Result.failure(AiError.NoInternet(e))
    } catch (e: SerializationException) {
        Result.failure(AiError.Unknown("Unexpected response", e))
    } catch (e: IllegalArgumentException) {
        // Includes an answer that is not valid JSON.
        Result.failure(AiError.Unknown("Unexpected response", e))
    }
