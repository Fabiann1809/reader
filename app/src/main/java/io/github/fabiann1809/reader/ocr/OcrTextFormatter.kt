package io.github.fabiann1809.reader.ocr

/**
 * Rebuilds readable paragraphs from OCR output.
 *
 * ML Kit returns blocks (roughly paragraphs) made of the physical lines printed on the page.
 * Lines of the same block are joined with spaces, and words split with a hyphen at the end of
 * a line are glued back together ("disper-" + "sarse" -> "dispersarse").
 */
fun formatRecognizedText(blocks: List<List<String>>): String =
    blocks
        .map { lines -> joinLines(lines.map(String::trim).filter(String::isNotEmpty)) }
        .filter(String::isNotEmpty)
        .joinToString(separator = "\n\n")

private fun joinLines(lines: List<String>): String {
    val result = StringBuilder()
    for (line in lines) {
        when {
            result.isEmpty() -> result.append(line)
            endsWithHyphenatedWord(result) -> {
                result.setLength(result.length - 1)
                result.append(line)
            }
            else -> result.append(' ').append(line)
        }
    }
    return result.toString()
}

// A trailing hyphen right after a letter means the word continues on the next line.
private fun endsWithHyphenatedWord(text: CharSequence): Boolean =
    text.length >= 2 && text.last() == '-' && text[text.length - 2].isLetter()
