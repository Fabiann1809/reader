package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.ai.AiProvider

/**
 * Configurable AiProvider for tests: returns [result] and records every text it was asked to explain.
 */
class FakeAiProvider(
    var result: Result<String> = Result.success("Fake explanation"),
) : AiProvider {

    val requests = mutableListOf<String>()

    override suspend fun explain(text: String): Result<String> {
        requests += text
        return result
    }
}
