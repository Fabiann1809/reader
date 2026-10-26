package io.github.fabiann1809.reader.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Turns a Gemini response schema (OpenAPI subset: `OBJECT`, `nullable`, `propertyOrdering`) into a
 * standard JSON Schema, which the other providers understand. The Gemini schemas stay the single
 * place where each answer's shape is written down.
 *
 * With [strict], as OpenAI's structured outputs ask: every property is required (the optional ones
 * may be null) and no other property is allowed.
 */
internal fun JsonObject.toJsonSchema(strict: Boolean, optional: Boolean = false): JsonObject {
    val nullable = optional || this["nullable"]?.jsonPrimitive?.booleanOrNull == true
    val requiredNames = (this["required"] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()
    val properties = this["properties"] as? JsonObject
    return buildJsonObject {
        for ((key, value) in this@toJsonSchema) {
            when (key) {
                // Not part of standard JSON Schema, or rebuilt below. Item counts are asked for in the prompt.
                "propertyOrdering", "nullable", "minItems", "maxItems", "required" -> Unit
                "type" -> {
                    val type = value.jsonPrimitive.content.lowercase()
                    if (nullable) putJsonArray("type") { add(type); add("null") } else put("type", type)
                }
                "properties" -> putJsonObject("properties") {
                    for ((name, property) in properties.orEmpty()) {
                        put(name, (property as JsonObject).toJsonSchema(strict, optional = strict && name !in requiredNames))
                    }
                }
                "items" -> put("items", (value as JsonObject).toJsonSchema(strict))
                else -> put(key, value)
            }
        }
        if (properties != null) {
            putJsonArray("required") { (if (strict) properties.keys else requiredNames).forEach { add(it) } }
            if (strict) put("additionalProperties", false)
        }
    }
}

private fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())
