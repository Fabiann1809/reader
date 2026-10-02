package io.github.fabiann1809.reader.ai

import kotlinx.serialization.Serializable

/**
 * A card proposed by the AI (T14.2) for the user to edit before saving. Providers ask the model
 * for this shape as JSON (field names are part of that contract).
 */
@Serializable
data class FlashcardDraft(
    /** A question or concept to recall. */
    val front: String,
    /** Its short answer or explanation. */
    val back: String,
)
