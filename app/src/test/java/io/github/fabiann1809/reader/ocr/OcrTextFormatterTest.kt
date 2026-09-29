package io.github.fabiann1809.reader.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextFormatterTest {

    @Test
    fun joinsLinesOfABlockWithSpaces() {
        val text = formatRecognizedText(listOf(listOf("La entropía es una", "medida del desorden.")))

        assertEquals("La entropía es una medida del desorden.", text)
    }

    @Test
    fun separatesBlocksWithBlankLine() {
        val text = formatRecognizedText(listOf(listOf("Capítulo 3"), listOf("Primer párrafo.")))

        assertEquals("Capítulo 3\n\nPrimer párrafo.", text)
    }

    @Test
    fun gluesWordsHyphenatedAtLineEnd() {
        val text = formatRecognizedText(listOf(listOf("la energía tiende a disper-", "sarse con el tiempo")))

        assertEquals("la energía tiende a dispersarse con el tiempo", text)
    }

    @Test
    fun keepsStandaloneDashes() {
        val text = formatRecognizedText(listOf(listOf("una pausa -", "y sigue")))

        assertEquals("una pausa - y sigue", text)
    }

    @Test
    fun dropsEmptyLinesAndBlocks() {
        val text = formatRecognizedText(listOf(listOf("  ", "Hola  "), emptyList(), listOf("")))

        assertEquals("Hola", text)
    }
}
