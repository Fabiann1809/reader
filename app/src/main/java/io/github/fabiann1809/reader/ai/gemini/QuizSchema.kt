package io.github.fabiann1809.reader.ai.gemini

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Gemini response schema matching [io.github.fabiann1809.reader.ai.Quiz]. */
internal val QuizSchema: JsonObject = buildJsonObject {
    put("type", "OBJECT")
    putJsonObject("properties") {
        putJsonObject("questions") {
            put("type", "ARRAY")
            putJsonObject("items") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("question") { put("type", "STRING") }
                    putJsonObject("options") {
                        put("type", "ARRAY")
                        putJsonObject("items") { put("type", "STRING") }
                        put("minItems", 4)
                        put("maxItems", 4)
                    }
                    putJsonObject("correctIndex") { put("type", "INTEGER") }
                    putJsonObject("explanation") { put("type", "STRING") }
                }
                putJsonArray("required") {
                    listOf("question", "options", "correctIndex", "explanation").forEach { add(it) }
                }
                putJsonArray("propertyOrdering") {
                    listOf("question", "options", "correctIndex", "explanation").forEach { add(it) }
                }
            }
        }
    }
    putJsonArray("required") { add("questions") }
}
