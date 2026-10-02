package io.github.fabiann1809.reader.ocr

/**
 * What the OCR read: the [text], and the [uncertainLines] it read with low confidence (e.g. from
 * a blurry photo), so the user can check them before going on (T12.4).
 */
data class RecognizedText(val text: String, val uncertainLines: List<String> = emptyList())

// Below this ML Kit confidence (0 to 1) a line is worth a second look; clear print scores far above.
const val UNCERTAIN_LINE_CONFIDENCE = 0.7f

/** The lines read with less than [threshold] confidence; lines without a confidence are trusted. */
fun uncertainLines(lines: List<OcrLine>, threshold: Float = UNCERTAIN_LINE_CONFIDENCE): List<String> =
    lines.filter { line -> line.text.isNotBlank() && (line.confidence ?: 1f) < threshold }.map { it.text.trim() }
