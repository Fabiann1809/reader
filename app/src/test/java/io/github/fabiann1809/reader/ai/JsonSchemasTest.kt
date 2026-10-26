package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.ai.gemini.ExplanationSchema
import io.github.fabiann1809.reader.ai.gemini.InterpretationSchema
import io.github.fabiann1809.reader.ai.gemini.QuizSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonSchemasTest {

    private fun JsonObject.props() = this["properties"]!!.jsonObject

    private fun JsonObject.type() = this["type"]!!

    @Test
    fun typesBecomeLowercaseAndGeminiOnlyKeywordsGoAway() {
        val schema = ExplanationSchema.toJsonSchema(strict = false)

        assertEquals(JsonPrimitive("object"), schema.type())
        assertEquals(JsonPrimitive("string"), schema.props()["mainIdea"]!!.jsonObject.type())
        assertFalse("propertyOrdering" in schema)
        assertFalse("nullable" in schema.props()["caveat"]!!.jsonObject)
    }

    @Test
    fun aNullablePropertyAcceptsNull() {
        val caveat = ExplanationSchema.toJsonSchema(strict = false).props()["caveat"]!!.jsonObject

        assertEquals(JsonArray(listOf(JsonPrimitive("string"), JsonPrimitive("null"))), caveat.type())
    }

    @Test
    fun withoutStrictOnlyTheRequiredPropertiesAreRequired() {
        val schema = ExplanationSchema.toJsonSchema(strict = false)

        assertEquals(listOf("mainIdea", "simpleExplanation", "analogy", "keyTerms"), schema["required"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertFalse("additionalProperties" in schema)
    }

    @Test
    fun strictRequiresEveryPropertyAndForbidsOthersAtEveryLevel() {
        val schema = ExplanationSchema.toJsonSchema(strict = true)

        assertEquals(5, schema["required"]!!.jsonArray.size)
        assertEquals(JsonPrimitive(false), schema["additionalProperties"])
        val term = schema.props()["keyTerms"]!!.jsonObject["items"]!!.jsonObject
        assertEquals(JsonPrimitive(false), term["additionalProperties"])
        assertEquals(2, term["required"]!!.jsonArray.size)
    }

    @Test
    fun strictMakesAnOptionalPropertyNullable() {
        val schema = InterpretationSchema.toJsonSchema(strict = true)

        assertEquals(3, schema["required"]!!.jsonArray.size)
        schema.props().values.forEach {
            assertTrue(it.jsonObject.type().jsonArray.contains(JsonPrimitive("null")))
        }
    }

    @Test
    fun itemCountsAreLeftToThePrompt() {
        val options = QuizSchema.toJsonSchema(strict = true).props()["questions"]!!.jsonObject["items"]!!
            .jsonObject.props()["options"]!!.jsonObject

        assertFalse("minItems" in options)
        assertEquals(JsonPrimitive("array"), options.type())
        assertEquals(JsonPrimitive("string"), options["items"]!!.jsonObject.type())
    }

    @Test
    fun theJsonInsideAMarkdownBlockIsFound() {
        assertEquals("{\"a\":1}", AiResponses.unwrapJson("```json\n{\"a\":1}\n```"))
        assertEquals("{\"a\":1}", AiResponses.unwrapJson("Aquí va: {\"a\":1} listo"))
        assertEquals("sin json", AiResponses.unwrapJson("sin json"))
    }
}
