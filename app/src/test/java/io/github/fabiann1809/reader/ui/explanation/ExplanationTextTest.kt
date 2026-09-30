package io.github.fabiann1809.reader.ui.explanation

import io.github.fabiann1809.reader.testing.testExplanation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExplanationTextTest {

    private val labels = ExplanationLabels("En una frase", "Explicado simple", "Analogía", "Palabras clave", "Ojo")

    @Test
    fun writesOneTitledParagraphPerBlock() {
        val text = testExplanation().toPlainText(labels)

        assertEquals(
            """
            En una frase: La luz es energía.

            Explicado simple: La luz lleva energía de un lugar a otro.

            Analogía: Como el calor que sientes al sol.

            Palabras clave:
            • Fotón: Partícula de luz.
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun addsTheCaveatOnlyWhenThereIsOne() {
        assertFalse(testExplanation().toPlainText(labels).contains("Ojo"))
        assertEquals(
            "Ojo: El fragmento está cortado.",
            testExplanation(caveat = "El fragmento está cortado.").toPlainText(labels).substringAfterLast("\n\n"),
        )
    }
}
