package io.github.fabiann1809.reader.ocr

/** A line of recognized text and its bounding box on the image, in pixels. */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Int get() = (top + bottom) / 2
}

/**
 * Rebuilds readable paragraphs from OCR lines using their position on the page.
 *
 * ML Kit's blocks don't always follow reading order (it may split one printed line into two
 * overlapping pieces), so instead:
 * 1. lines at the same height form a row, read left to right;
 * 2. rows are read top to bottom and joined with spaces;
 * 3. rows much further apart than usual start a new paragraph;
 * 4. words split with a hyphen at the end of a row are glued back ("disper-" + "sarse").
 */
fun formatRecognizedText(lines: List<OcrLine>): String {
    val rows = groupIntoRows(lines.filter { it.text.isNotBlank() })
    if (rows.isEmpty()) return ""

    val distances = rows.zipWithNext { a, b -> b.centerY - a.centerY }
    val paragraphThreshold = if (distances.size >= 2) {
        // Distance between row centers is stable even when letters with ascenders/descenders
        // change the height of each line's box.
        distances.median() * PARAGRAPH_DISTANCE_RATIO
    } else {
        rows.map { it.bottom - it.top }.median() * SHORT_TEXT_DISTANCE_RATIO
    }

    val paragraphs = mutableListOf(mutableListOf(rows.first()))
    for ((index, row) in rows.drop(1).withIndex()) {
        if (distances[index] > paragraphThreshold) paragraphs += mutableListOf(row) else paragraphs.last() += row
    }
    return paragraphs.joinToString(separator = "\n\n") { paragraph -> joinRows(paragraph.map { it.text }) }
}

// A row this many times further than the usual row distance starts a new paragraph.
private const val PARAGRAPH_DISTANCE_RATIO = 1.5

// With only two rows there is no "usual" distance; compare against the line height instead.
private const val SHORT_TEXT_DISTANCE_RATIO = 2.0

private class Row(val text: String, val top: Int, val bottom: Int) {
    val centerY: Int get() = (top + bottom) / 2
}

// Lower median: paragraph breaks are the large outliers, so this represents normal line spacing.
private fun List<Int>.median(): Double = sorted()[(size - 1) / 2].toDouble()

private fun groupIntoRows(lines: List<OcrLine>): List<Row> {
    val rows = mutableListOf<MutableList<OcrLine>>()
    for (line in lines.sortedBy { it.top }) {
        val current = rows.lastOrNull()
        // Same row if this line's vertical center falls inside the row's current height band.
        if (current != null && line.centerY in current.minOf { it.top }..current.maxOf { it.bottom }) {
            current += line
        } else {
            rows += mutableListOf(line)
        }
    }
    return rows.map { row ->
        Row(text = joinPieces(row.sortedBy { it.left }), top = row.minOf { it.top }, bottom = row.maxOf { it.bottom })
    }
}

/** Joins pieces of one printed line, dropping a word both pieces share where they overlap. */
private fun joinPieces(pieces: List<OcrLine>): String {
    val words = mutableListOf<String>()
    var previousRight = Int.MIN_VALUE
    for (piece in pieces) {
        val pieceWords = piece.text.trim().split(Regex("\\s+")).toMutableList()
        val overlaps = piece.left < previousRight
        if (overlaps && words.isNotEmpty() && pieceWords.first().equals(words.last(), ignoreCase = true)) {
            pieceWords.removeAt(0)
        }
        words += pieceWords
        previousRight = maxOf(previousRight, piece.right)
    }
    return words.joinToString(" ")
}

private fun joinRows(rows: List<String>): String {
    val result = StringBuilder()
    for (row in rows) {
        when {
            result.isEmpty() -> result.append(row)
            endsWithHyphenatedWord(result) -> {
                result.setLength(result.length - 1)
                result.append(row)
            }
            else -> result.append(' ').append(row)
        }
    }
    return result.toString()
}

// A trailing hyphen right after a letter means the word continues on the next row.
private fun endsWithHyphenatedWord(text: CharSequence): Boolean =
    text.length >= 2 && text.last() == '-' && text[text.length - 2].isLetter()
