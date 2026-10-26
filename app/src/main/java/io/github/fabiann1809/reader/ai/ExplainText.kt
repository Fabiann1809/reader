package io.github.fabiann1809.reader.ai

/** Roughly one book page: keeps each AI request small (and cheap for the user's quota). */
const val MAX_TEXT_LENGTH = 4_000

/**
 * Use case: explains a book fragment with the Feynman prompt (set as the provider's system
 * instruction, see [ExplainerPrompt]).
 *
 * Invalid fragments fail with [IllegalArgumentException] before any request is made,
 * so they never cost the user quota.
 */
class ExplainText(private val aiProvider: AiProvider) {

    suspend operator fun invoke(text: String): Result<Explanation> = checked(text) { aiProvider.explain(it) }

    /** "Más simple" and "Otro ejemplo": the same fragment explained again (T12.1). */
    suspend fun refine(text: String, previous: Explanation, change: ExplanationChange): Result<Explanation> =
        checked(text) { aiProvider.refine(it, previous, change) }

    private suspend fun checked(text: String, explain: suspend (String) -> Result<Explanation>): Result<Explanation> {
        val fragment = text.trim()
        return when {
            fragment.isEmpty() -> Result.failure(IllegalArgumentException("Text is blank"))
            fragment.length > MAX_TEXT_LENGTH -> Result.failure(IllegalArgumentException("Text is too long"))
            else -> explain(fragment)
        }
    }
}
