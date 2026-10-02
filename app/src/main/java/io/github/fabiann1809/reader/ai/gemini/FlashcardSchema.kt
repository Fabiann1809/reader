package io.github.fabiann1809.reader.ai.gemini

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Gemini response schema matching [io.github.fabiann1809.reader.ai.FlashcardDraft]. */
internal val FlashcardSchema: JsonObject = buildJsonObject {
    put("type", "OBJECT")
    putJsonObject("properties") {
        putJsonObject("front") { put("type", "STRING") }
        putJsonObject("back") { put("type", "STRING") }
    }
    putJsonArray("required") {
        add("front")
        add("back")
    }
    putJsonArray("propertyOrdering") {
        add("front")
        add("back")
    }
}
