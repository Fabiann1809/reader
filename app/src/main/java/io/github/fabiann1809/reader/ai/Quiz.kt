package io.github.fabiann1809.reader.ai

import kotlinx.serialization.Serializable

/**
 * A multiple choice quiz about something the reader read (T15.2). Providers ask the model for this
 * shape as JSON (field names are part of that contract).
 */
@Serializable
data class Quiz(val questions: List<QuizQuestion>)

@Serializable
data class QuizQuestion(
    val question: String,
    /** Always four options. */
    val options: List<String>,
    /** Index of the right option in [options]. */
    val correctIndex: Int,
    /** Why the right option is right, according to the text. */
    val explanation: String,
    /** A two or three word name of the idea it asks about, grouping the result's topics (T15.4). */
    val topic: String = "",
)

/** Questions per quiz the reader can ask for (design 01: 3, 5 or 10). */
val QUIZ_SIZES = listOf(3, 5, 10)

const val QUIZ_OPTIONS = 4
