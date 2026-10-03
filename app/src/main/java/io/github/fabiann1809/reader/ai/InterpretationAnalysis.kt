package io.github.fabiann1809.reader.ai

import kotlinx.serialization.Serializable

/**
 * What the AI thinks of the reader's own words about a text ("Ahora tú", T15.1): what they got
 * right, what is missing and what contradicts the text. Each part is null when there is nothing
 * to say. Providers ask the model for this shape as JSON (field names are part of that contract).
 */
@Serializable
data class InterpretationAnalysis(
    val understood: String? = null,
    val incomplete: String? = null,
    val confused: String? = null,
)
