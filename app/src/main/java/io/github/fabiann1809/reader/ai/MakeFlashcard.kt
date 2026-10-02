package io.github.fabiann1809.reader.ai

/**
 * Use case: asks the AI to propose a card from [text] (T14.2). Like [ExplainText], invalid text
 * fails with [IllegalArgumentException] before any request, so it never costs the user quota.
 */
class MakeFlashcard(private val aiProvider: AiProvider) {

    suspend operator fun invoke(text: String): Result<FlashcardDraft> {
        val source = text.trim()
        return when {
            source.isEmpty() -> Result.failure(IllegalArgumentException("Text is blank"))
            source.length > MAX_TEXT_LENGTH -> Result.failure(IllegalArgumentException("Text is too long"))
            else -> aiProvider.makeFlashcard(source)
        }
    }
}
