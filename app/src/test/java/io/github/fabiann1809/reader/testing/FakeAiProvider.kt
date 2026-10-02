package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.Explanation

/**
 * Configurable AiProvider for tests: returns [result] and records every text it was asked to explain.
 */
class FakeAiProvider(
    var result: Result<Explanation> = Result.success(testExplanation()),
) : AiProvider {

    val requests = mutableListOf<String>()

    override suspend fun explain(text: String): Result<Explanation> {
        requests += text
        return result
    }

    var transcription: Result<String> = Result.success("Una idea sobre el capítulo")
    val audios = mutableListOf<Pair<ByteArray, String>>()

    override suspend fun transcribe(audio: ByteArray, mimeType: String): Result<String> {
        audios += audio to mimeType
        return transcription
    }
}
