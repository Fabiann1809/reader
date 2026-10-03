package io.github.fabiann1809.reader.ai

/**
 * Use case: compares the reader's own words with the text they read ("Ahora tú", T15.1). Blank or
 * too long input fails with [IllegalArgumentException] before any request, so it costs no quota.
 */
class AnalyzeInterpretation(private val aiProvider: AiProvider) {

    suspend operator fun invoke(text: String, interpretation: String): Result<InterpretationAnalysis> {
        val source = text.trim()
        val ownWords = interpretation.trim()
        return when {
            source.isEmpty() || ownWords.isEmpty() -> Result.failure(IllegalArgumentException("Nothing to compare"))
            source.length > MAX_TEXT_LENGTH || ownWords.length > MAX_TEXT_LENGTH ->
                Result.failure(IllegalArgumentException("Text is too long"))
            else -> aiProvider.analyzeInterpretation(source, ownWords)
        }
    }
}
