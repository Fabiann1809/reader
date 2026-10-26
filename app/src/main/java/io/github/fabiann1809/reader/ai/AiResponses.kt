package io.github.fabiann1809.reader.ai

import kotlinx.serialization.json.Json

/**
 * Reads the JSON a model answered with into the app's types and tidies it. Every provider uses it,
 * so they all give the same shapes. A malformed answer throws a `SerializationException`, which
 * the provider reports as an [AiError].
 */
internal object AiResponses {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** The JSON object in [text]: some models wrap it in a Markdown code block or add words around it. */
    fun unwrapJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start in 0 until end) text.substring(start, end + 1) else text
    }

    fun explanation(text: String): Explanation = json.decodeFromString<Explanation>(text).let {
        // Models sometimes return "" instead of null for an empty caveat.
        it.copy(
            mainIdea = it.mainIdea.trim(),
            simpleExplanation = it.simpleExplanation.trim(),
            analogy = it.analogy.trim(),
            caveat = it.caveat.orBlankAsNull(),
        )
    }

    fun flashcard(text: String): FlashcardDraft =
        json.decodeFromString<FlashcardDraft>(text).let { FlashcardDraft(it.front.trim(), it.back.trim()) }

    fun interpretation(text: String): InterpretationAnalysis {
        val analysis = json.decodeFromString<InterpretationAnalysis>(text)
        // Models sometimes write "" instead of leaving a part out.
        return InterpretationAnalysis(
            understood = analysis.understood.orBlankAsNull(),
            incomplete = analysis.incomplete.orBlankAsNull(),
            confused = analysis.confused.orBlankAsNull(),
        )
    }

    fun quiz(text: String): Quiz = Quiz(
        json.decodeFromString<Quiz>(text).questions.map { question ->
            question.copy(
                question = question.question.trim(),
                options = question.options.map { it.trim() },
                explanation = question.explanation.trim(),
                topic = question.topic.trim(),
            )
        },
    )

    private fun String?.orBlankAsNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
