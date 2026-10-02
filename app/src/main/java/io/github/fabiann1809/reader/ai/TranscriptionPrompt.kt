package io.github.fabiann1809.reader.ai

/**
 * System instruction for transcribing a voice note (T13.2). In Spanish, like the explainer's,
 * because the notes are; the model keeps the language that is spoken.
 */
object TranscriptionPrompt {

    val SYSTEM_INSTRUCTION = """
        Transcribe palabra por palabra lo que se dice en el audio, en el idioma en que se habla.
        Es una nota de voz que alguien grabó mientras leía un libro.

        Reglas:
        - Devuelve solo la transcripción: sin comentarios, títulos, etiquetas ni marcas de tiempo.
        - Usa ortografía correcta, con tildes y signos de puntuación.
        - Quita muletillas y repeticiones sin sentido ("eh", "este..."), pero no cambies las ideas.
        - Si no se oye ninguna palabra, responde con un texto vacío.
    """.trimIndent()
}
