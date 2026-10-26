package io.github.fabiann1809.reader.ai

/**
 * System instruction for the Feynman explainer (spec, Annex A).
 *
 * Written in Spanish because it shapes the explanation the user reads; the model still answers
 * in the language of the fragment. The block structure itself is enforced by the provider's
 * JSON schema (see [Explanation]); this text says what goes in each field.
 */
object ExplainerPrompt {

    val SYSTEM_INSTRUCTION = """
        Eres un tutor que explica textos difíciles con el método Feynman.
        Recibirás un fragmento de un libro. Responde en el mismo idioma del fragmento, rellenando estos campos:

        - mainIdea: la idea central en una sola frase, sin jerga (máximo 25 palabras).
        - simpleExplanation: explícalo como si hablaras con alguien sin conocimientos previos (60 a 120 palabras).
        - analogy: una comparación con algo de la vida diaria (1 a 3 frases).
        - keyTerms: de 2 a 5 términos difíciles del texto, cada uno con una definición de una línea. Escribe cada término tal como aparece en el fragmento, con sus tildes.
        - caveat: si el fragmento es ambiguo, está incompleto o parece mal leído, explícalo brevemente; si no, déjalo vacío.

        Reglas:
        - No inventes información que no esté en el texto.
        - Usa ortografía correcta, con todas las tildes y signos del idioma.
        - Escribe en texto plano, sin formato Markdown (sin asteriscos ni almohadillas).
    """.trimIndent()

    /** The user's message to change an explanation: the text, what was said about it, and what to do. */
    fun refineMessage(text: String, previous: Explanation, change: ExplanationChange): String {
        val request = when (change) {
            ExplanationChange.SIMPLER ->
                "Explícalo de nuevo con palabras todavía más simples y frases más cortas, como a alguien sin ningún conocimiento previo. Mantén las mismas ideas."
            ExplanationChange.ANOTHER_EXAMPLE ->
                "Explícalo de nuevo con una analogía y un ejemplo distintos de los anteriores, de otro ámbito de la vida diaria. Mantén las mismas ideas."
        }
        return "Fragmento:\n$text\n\nExplicación anterior:\n" +
            "Idea central: ${previous.mainIdea}\n" +
            "Explicación sencilla: ${previous.simpleExplanation}\n" +
            "Analogía: ${previous.analogy}\n\n" +
            request
    }
}
