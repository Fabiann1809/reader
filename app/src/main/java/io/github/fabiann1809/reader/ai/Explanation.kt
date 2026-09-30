package io.github.fabiann1809.reader.ai

import kotlinx.serialization.Serializable

/**
 * A Feynman explanation split into the explainer's blocks, so the UI can show each one with its own style.
 * Providers ask the model for this shape as JSON (field names are part of that contract).
 */
@Serializable
data class Explanation(
    /** The core idea in one sentence. */
    val mainIdea: String,
    /** The idea told to someone with no background, without jargon. */
    val simpleExplanation: String,
    /** A comparison with everyday life. */
    val analogy: String,
    val keyTerms: List<KeyTerm> = emptyList(),
    /** Set when the fragment is ambiguous or incomplete; null otherwise. */
    val caveat: String? = null,
)

@Serializable
data class KeyTerm(
    val term: String,
    val definition: String,
)
