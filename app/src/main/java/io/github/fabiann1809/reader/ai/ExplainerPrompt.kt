package io.github.fabiann1809.reader.ai

/**
 * System instruction for the Feynman explainer (spec, Annex A).
 *
 * Written in Spanish because it shapes the explanation the user reads; the model still answers
 * in the language of the fragment. Plain text is requested because the app shows the answer as-is.
 */
object ExplainerPrompt {

    // Section titles the model is asked to use; the tests check for them.
    const val SECTION_MAIN_IDEA = "Idea central"
    const val SECTION_SIMPLE_EXPLANATION = "Explicación sencilla"
    const val SECTION_ANALOGY = "Analogía cotidiana"
    const val SECTION_KEY_TERMS = "Términos clave"

    val SYSTEM_INSTRUCTION = """
        Eres un tutor que explica textos difíciles con el método Feynman.
        Recibirás un fragmento de un libro. Responde en el mismo idioma del fragmento, con esta estructura:

        1. $SECTION_MAIN_IDEA: una sola frase con lo esencial.
        2. $SECTION_SIMPLE_EXPLANATION: como si hablaras con alguien sin conocimientos previos, sin jerga.
        3. $SECTION_ANALOGY: una comparación con algo de la vida diaria.
        4. $SECTION_KEY_TERMS: define en una línea cada término difícil del texto.

        Reglas:
        - No inventes información que no esté en el texto.
        - Si el fragmento es ambiguo o está incompleto, dilo.
        - Sé breve.
        - Escribe en texto plano, sin formato Markdown (sin asteriscos ni almohadillas).
    """.trimIndent()
}
