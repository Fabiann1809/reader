package io.github.fabiann1809.reader.ocr

import io.github.fabiann1809.reader.ui.extractedtext.uncertainRanges
import org.junit.Assert.assertEquals
import org.junit.Test

class RecognizedTextTest {

    private fun line(text: String, confidence: Float?) = OcrLine(text, left = 0, top = 0, right = 10, bottom = 10, confidence = confidence)

    @Test
    fun onlyLinesUnderTheThresholdAreUncertain() {
        val lines = listOf(line("Capítulo 3", 0.98f), line("una medlda", 0.41f), line("sin dato", null), line("  ", 0.1f))

        assertEquals(listOf("una medlda"), uncertainLines(lines))
    }

    @Test
    fun everyAppearanceOfADoubtedLineIsMarked() {
        val text = "ab cd ab"

        assertEquals(listOf(0..1, 6..7), uncertainRanges(text, listOf("ab")))
        assertEquals(emptyList<IntRange>(), uncertainRanges(text, listOf("zz", " ")))
    }
}
