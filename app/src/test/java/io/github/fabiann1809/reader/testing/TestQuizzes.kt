package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.ai.Quiz
import io.github.fabiann1809.reader.ai.QuizQuestion

/** A valid quiz of [count] questions whose right answer is always the second option. */
fun testQuiz(count: Int) = Quiz(
    List(count) { index ->
        QuizQuestion(
            question = "¿Pregunta ${index + 1}?",
            options = listOf("A", "B", "C", "D"),
            correctIndex = 1,
            explanation = "Porque lo dice el texto.",
        )
    },
)
