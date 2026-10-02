package io.github.fabiann1809.reader.ui.extractedtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Paints a background behind each line the OCR doubted (T12.4), wherever it still appears
 * unchanged in the text. Only the look changes: the text being edited stays the same, so the
 * cursor and the offsets are untouched. Editing a line makes its mark go away.
 */
class UncertainLinesHighlight(private val lines: List<String>, private val color: Color) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val marked = AnnotatedString.Builder(text).apply {
            uncertainRanges(text.text, lines).forEach { range ->
                addStyle(SpanStyle(background = color), range.first, range.last + 1)
            }
        }.toAnnotatedString()
        return TransformedText(marked, OffsetMapping.Identity)
    }

    // A different set of lines must paint again, so equality follows them.
    override fun equals(other: Any?): Boolean = other is UncertainLinesHighlight && other.lines == lines && other.color == color

    override fun hashCode(): Int = lines.hashCode() * 31 + color.hashCode()
}

/** Where each of [lines] appears in [text] (every time it does), as character ranges. */
fun uncertainRanges(text: String, lines: List<String>): List<IntRange> = lines.filter { it.isNotBlank() }.flatMap { line ->
    generateSequence(text.indexOf(line).takeIf { it >= 0 }) { from -> text.indexOf(line, from + line.length).takeIf { it >= 0 } }
        .map { start -> start until start + line.length }
        .toList()
}
