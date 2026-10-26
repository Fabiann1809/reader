package io.github.fabiann1809.reader.ai

/** Stands in for a provider the app can't talk to yet: every call fails with an [AiError]. */
class UnavailableAiProvider(private val name: String) : AiProvider {

    private fun <T> unavailable(): Result<T> = Result.failure(AiError.Unknown("$name is not available yet"))

    override suspend fun explain(text: String) = unavailable<Explanation>()

    override suspend fun refine(text: String, previous: Explanation, change: ExplanationChange) = unavailable<Explanation>()

    override suspend fun transcribe(audio: ByteArray, mimeType: String) = unavailable<String>()

    override suspend fun makeFlashcard(text: String) = unavailable<FlashcardDraft>()

    override suspend fun analyzeInterpretation(text: String, interpretation: String) = unavailable<InterpretationAnalysis>()

    override suspend fun generateQuiz(text: String, questionCount: Int) = unavailable<Quiz>()
}
