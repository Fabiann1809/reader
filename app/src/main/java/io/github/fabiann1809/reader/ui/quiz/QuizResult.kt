package io.github.fabiann1809.reader.ui.quiz

import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizQuestion

/**
 * How a quiz went (T15.4): the score, the topics answered right every time ([strongTopics]), those
 * with some mistake ([weakTopics]) and the [missed] questions, which can become cards.
 */
data class QuizResult(
    val correct: Int,
    val total: Int,
    val strongTopics: List<String>,
    val weakTopics: List<String>,
    val missed: List<QuizQuestion>,
) {
    companion object {
        /** The result of answering [quiz] with [answers] (the option picked for each question, in order). */
        fun of(quiz: Quiz, answers: List<Int>): QuizResult {
            val graded = quiz.questions.zip(answers) { question, answer -> question to (answer == question.correctIndex) }
            val missed = graded.filterNot { it.second }.map { it.first }
            val byTopic = graded.filter { it.first.topic.isNotBlank() }.groupBy({ it.first.topic }, { it.second })
            return QuizResult(
                correct = graded.count { it.second },
                total = quiz.questions.size,
                strongTopics = byTopic.filterValues { results -> results.all { it } }.keys.toList(),
                weakTopics = byTopic.filterValues { results -> !results.all { it } }.keys.toList(),
                missed = missed,
            )
        }
    }
}
