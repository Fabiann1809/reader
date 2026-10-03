package io.github.fabiann1809.reader.ai.gemini

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Gemini response schema matching [io.github.fabiann1809.reader.ai.InterpretationAnalysis]. */
internal val InterpretationSchema: JsonObject = buildJsonObject {
    put("type", "OBJECT")
    putJsonObject("properties") {
        listOf("understood", "incomplete", "confused").forEach { field ->
            putJsonObject(field) {
                put("type", "STRING")
                put("nullable", true)
            }
        }
    }
    putJsonArray("propertyOrdering") {
        listOf("understood", "incomplete", "confused").forEach { add(it) }
    }
}
