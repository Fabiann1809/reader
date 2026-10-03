package io.github.fabiann1809.reader.ai

// About ten book pages: a whole chapter fits, and the request stays within reasonable quota use.
const val MAX_QUIZ_TEXT_LENGTH = 30_000

/**
 * Use case: asks the AI for a quiz of [questionCount] questions about a text (T15.2). Invalid input
 * fails with [IllegalArgumentException] before any request; an answer that isn't a usable quiz
 * (wrong number of questions or options, a correct answer out of range) fails with [AiError.Unknown].
 */
class GenerateQuiz(private val aiProvider: AiProvider) {

    suspend operator fun invoke(text: String, questionCount: Int): Result<Quiz> {
        val source = text.trim()
        return when {
            questionCount !in QUIZ_SIZES -> Result.failure(IllegalArgumentException("Unsupported quiz size"))
            source.isEmpty() -> Result.failure(IllegalArgumentException("Text is blank"))
            source.length > MAX_QUIZ_TEXT_LENGTH -> Result.failure(IllegalArgumentException("Text is too long"))
            else -> aiProvider.generateQuiz(source, questionCount).mapCatching { quiz -> quiz.checked(questionCount) }
        }
    }

    private fun Quiz.checked(questionCount: Int): Quiz {
        val usable = questions.filter { it.isUsable() }
        // A model may add an extra question; fewer than asked means it didn't follow the format.
        if (usable.size < questionCount) throw AiError.Unknown("The quiz came back incomplete")
        return Quiz(usable.take(questionCount))
    }

    private fun QuizQuestion.isUsable(): Boolean =
        question.isNotBlank() &&
            options.size == QUIZ_OPTIONS &&
            options.all { it.isNotBlank() } &&
            correctIndex in options.indices &&
            explanation.isNotBlank()
}
