package io.github.fabiann1809.reader.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextFormatterTest {

    // Lines about 45 px tall, printed every 64 px, like a typical book page scan.
    private fun line(
        text: String,
        row: Int,
        left: Int = 120,
        right: Int = 1100,
        topOffset: Int = 0,
        bottomOffset: Int = 0,
    ): OcrLine {
        val top = 320 + row * 64
        return OcrLine(text = text, left = left, top = top + topOffset, right = right, bottom = top + 45 + bottomOffset)
    }

    @Test
    fun joinsConsecutiveLinesWithSpaces() {
        val text = formatRecognizedText(listOf(line("La entropía es una", 0), line("medida del desorden.", 1)))

        assertEquals("La entropía es una medida del desorden.", text)
    }

    @Test
    fun ordersLinesTopToBottomRegardlessOfInputOrder() {
        val text = formatRecognizedText(listOf(line("segunda", 1), line("primera", 0)))

        assertEquals("primera segunda", text)
    }

    @Test
    fun mergesPiecesOfTheSamePrintedLineLeftToRight() {
        // ML Kit sometimes returns one printed line as two separate pieces.
        val text = formatRecognizedText(
            listOf(
                line("es una medida del desorden de un", 0, left = 400),
                line("La entropía", 0, left = 120, right = 380),
                line("sistema.", 1),
            ),
        )

        assertEquals("La entropía es una medida del desorden de un sistema.", text)
    }

    @Test
    fun dropsWordRepeatedWhereTwoPiecesOverlap() {
        // Real case: "La entropía es" and "es una medida…" overlap on the word "es".
        val text = formatRecognizedText(
            listOf(
                line("La entropía es", 0, left = 120, right = 460),
                line("es una medida", 0, left = 420, right = 800),
            ),
        )

        assertEquals("La entropía es una medida", text)
    }

    @Test
    fun keepsRepeatedWordWhenPiecesDoNotOverlap() {
        val text = formatRecognizedText(
            listOf(line("dijo que", 0, left = 120, right = 300), line("que no", 0, left = 320, right = 500)),
        )

        assertEquals("dijo que que no", text)
    }

    @Test
    fun unevenLineBoxesDoNotCreateFalseParagraphs() {
        // Lines without ascenders/descenders have shorter boxes; the gap between boxes varies but
        // the distance between line centers stays the same.
        val text = formatRecognizedText(
            listOf(
                line("uno", 0),
                line("dos", 1, bottomOffset = -12),
                line("tres", 2, topOffset = 12),
                line("cuatro", 3),
            ),
        )

        assertEquals("uno dos tres cuatro", text)
    }

    @Test
    fun bigVerticalGapStartsNewParagraph() {
        val title = OcrLine("Capítulo 3", left = 120, top = 160, right = 900, bottom = 216)
        val text = formatRecognizedText(listOf(title, line("Primer párrafo", 0), line("sigue aquí.", 1)))

        assertEquals("Capítulo 3\n\nPrimer párrafo sigue aquí.", text)
    }

    @Test
    fun bigGapWithOnlyTwoRowsStartsNewParagraph() {
        val title = OcrLine("Capítulo 3", left = 120, top = 160, right = 900, bottom = 216)

        assertEquals("Capítulo 3\n\nTexto.", formatRecognizedText(listOf(title, line("Texto.", 0))))
    }

    @Test
    fun gluesWordsHyphenatedAtLineEnd() {
        val text = formatRecognizedText(listOf(line("tiende a disper-", 0), line("sarse con el tiempo", 1)))

        assertEquals("tiende a dispersarse con el tiempo", text)
    }

    @Test
    fun keepsStandaloneDashes() {
        val text = formatRecognizedText(listOf(line("una pausa -", 0), line("y sigue", 1)))

        assertEquals("una pausa - y sigue", text)
    }

    @Test
    fun ignoresBlankLinesAndEmptyInput() {
        assertEquals("", formatRecognizedText(emptyList()))
        assertEquals("Hola", formatRecognizedText(listOf(line("   ", 0), line(" Hola ", 1))))
    }
}
