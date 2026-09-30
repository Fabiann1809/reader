package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.ai.Explanation
import io.github.fabiann1809.reader.ai.KeyTerm

/** A complete explanation for tests; override only what a test cares about. */
fun testExplanation(
    mainIdea: String = "La luz es energía.",
    caveat: String? = null,
) = Explanation(
    mainIdea = mainIdea,
    simpleExplanation = "La luz lleva energía de un lugar a otro.",
    analogy = "Como el calor que sientes al sol.",
    keyTerms = listOf(KeyTerm("Fotón", "Partícula de luz.")),
    caveat = caveat,
)
