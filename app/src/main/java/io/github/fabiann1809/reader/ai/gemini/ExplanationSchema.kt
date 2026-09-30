package io.github.fabiann1809.reader.ai.gemini

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Gemini response schema (OpenAPI subset) matching [io.github.fabiann1809.reader.ai.Explanation].
 * propertyOrdering keeps the model writing the blocks in the order a person would read them.
 */
internal val ExplanationSchema: JsonObject = buildJsonObject {
    put("type", "OBJECT")
    putJsonObject("properties") {
        putJsonObject("mainIdea") { put("type", "STRING") }
        putJsonObject("simpleExplanation") { put("type", "STRING") }
        putJsonObject("analogy") { put("type", "STRING") }
        putJsonObject("keyTerms") {
            put("type", "ARRAY")
            putJsonObject("items") {
                put("type", "OBJECT")
                putJsonObject("properties") {
                    putJsonObject("term") { put("type", "STRING") }
                    putJsonObject("definition") { put("type", "STRING") }
                }
                putJsonArray("required") {
                    add("term")
                    add("definition")
                }
            }
        }
        putJsonObject("caveat") {
            put("type", "STRING")
            put("nullable", true)
        }
    }
    putJsonArray("required") {
        add("mainIdea")
        add("simpleExplanation")
        add("analogy")
        add("keyTerms")
    }
    putJsonArray("propertyOrdering") {
        listOf("mainIdea", "simpleExplanation", "analogy", "keyTerms", "caveat").forEach { add(it) }
    }
}
