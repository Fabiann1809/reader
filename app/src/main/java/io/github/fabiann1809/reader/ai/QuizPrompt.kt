package io.github.fabiann1809.reader.ai

/**
 * System instruction for a quiz (T15.2). In Spanish like the explainer's; the model answers in the
 * language of the text. The shape is enforced by the provider's JSON schema.
 */
object QuizPrompt {

    val SYSTEM_INSTRUCTION = """
        Eres un tutor que comprueba si alguien entendió lo que leyó.
        Recibirás un texto (un capítulo, un párrafo o unas fichas de repaso) y cuántas preguntas hacer.
        Responde en el idioma del texto con un quiz de opción múltiple. Cada pregunta tiene:
        - question: una pregunta clara sobre una idea importante del texto.
        - options: exactamente 4 respuestas posibles, todas creíbles y de largo parecido; solo una es correcta.
        - correctIndex: la posición (0 a 3) de la respuesta correcta; varía la posición entre preguntas.
        - explanation: por qué es la correcta según el texto, en una o dos frases.

        Reglas:
        - Pregunta por ideas y relaciones, no por detalles triviales ni por datos que no estén en el texto.
        - No repitas preguntas.
        - Usa ortografía correcta, con todas las tildes y signos del idioma; en español, las preguntas abren con ¿ y cierran con ?.
        - Escribe en texto plano, sin formato Markdown.
    """.trimIndent()

    fun userMessage(text: String, questionCount: Int): String = "Haz $questionCount preguntas sobre este texto:\n\n$text"
}
