package io.github.fabiann1809.reader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.theme.CoverStyle
import io.github.fabiann1809.reader.ui.theme.DmSerifDisplay

/** The drawing above the title of a generated cover. Which one a book gets is fixed by its title. */
enum class CoverPattern { BAND, CIRCLES, DIAGONALS, SUN, DOTS, PANEL }

/** Where the title and the author sit on a generated cover. */
enum class CoverLayout { TITLE_BELOW, AUTHOR_BELOW, CENTERED }

// Taken from the title's hash apart from the color's (which uses the hash modulo the palette),
// so the same color does not always come with the same drawing.
private const val COLOR_COUNT = 12

fun coverPattern(title: String): CoverPattern =
    CoverPattern.entries[Math.floorMod(Math.floorDiv(title.hashCode(), COLOR_COUNT), CoverPattern.entries.size)]

fun coverLayout(title: String): CoverLayout =
    CoverLayout.entries[Math.floorMod(Math.floorDiv(title.hashCode(), COLOR_COUNT * CoverPattern.entries.size), CoverLayout.entries.size)]

/** The pattern, then the author and the title in the cover's ink. */
@Composable
fun GeneratedCover(book: Book, style: CoverStyle, titleSize: TextUnit) {
    // A thumbnail has no room for a drawing and a title: it keeps only the band.
    val pattern = if (titleSize < MIN_PATTERN_TITLE_SIZE) CoverPattern.BAND else coverPattern(book.title)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawPattern(pattern, style) }
        CoverTexts(book, style, titleSize, coverLayout(book.title), pattern.reservedHeight())
    }
}

@Composable
private fun CoverTexts(book: Book, style: CoverStyle, titleSize: TextUnit, layout: CoverLayout, reserved: Float) {
    val centered = layout == CoverLayout.CENTERED
    val author = @Composable {
        if (book.author.isNotBlank()) {
            Text(
                text = book.author.uppercase(),
                color = style.ink.copy(alpha = 0.8f),
                fontSize = titleSize * AUTHOR_SIZE_RATIO,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.12.em,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val title = @Composable {
        Text(
            text = book.title,
            color = style.ink,
            fontFamily = DmSerifDisplay,
            fontSize = titleSize,
            lineHeight = titleSize * 1.05f,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Column(Modifier.fillMaxSize()) {
        // The pattern's room is left free: the texts start under it.
        if (reserved > 0f) Spacer(Modifier.fillMaxHeight(reserved))
        Column(
            verticalArrangement = when (layout) {
                CoverLayout.TITLE_BELOW -> Arrangement.SpaceBetween
                CoverLayout.AUTHOR_BELOW -> Arrangement.spacedBy(6.dp, Alignment.Bottom)
                CoverLayout.CENTERED -> Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
            },
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 10.dp, end = 8.dp, top = if (reserved > 0f) 8.dp else 24.dp, bottom = 12.dp),
        ) {
            when (layout) {
                CoverLayout.TITLE_BELOW -> {
                    author()
                    title()
                }
                CoverLayout.AUTHOR_BELOW, CoverLayout.CENTERED -> {
                    title()
                    author()
                }
            }
        }
    }
}

/** The share of the cover's height that a pattern takes at the top (0 when it sits behind the texts). */
private fun CoverPattern.reservedHeight(): Float = when (this) {
    CoverPattern.BAND, CoverPattern.CIRCLES -> 0f
    CoverPattern.DIAGONALS -> 0.42f
    CoverPattern.SUN -> 0.38f
    CoverPattern.DOTS -> 0.4f
    CoverPattern.PANEL -> 0.38f
}

private const val AUTHOR_SIZE_RATIO = 0.55f
private val MIN_PATTERN_TITLE_SIZE = 9.sp

private fun DrawScope.drawPattern(pattern: CoverPattern, style: CoverStyle) {
    when (pattern) {
        CoverPattern.BAND -> drawRect(style.band, Offset(0f, 12.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
        CoverPattern.CIRCLES -> drawCircles(style)
        CoverPattern.DIAGONALS -> drawDiagonals(style)
        CoverPattern.SUN -> drawSun(style)
        CoverPattern.DOTS -> drawDots(style)
        CoverPattern.PANEL -> drawPanel(style)
    }
}

// Rings around a corner, as if a coin were laid on the cover.
private fun DrawScope.drawCircles(style: CoverStyle) {
    val center = Offset(size.width * 0.82f, size.height * 0.2f)
    listOf(0.22f to 0.7f, 0.4f to 0.45f, 0.58f to 0.25f).forEach { (radius, alpha) ->
        drawCircle(style.band.copy(alpha = alpha), radius = size.width * radius, center = center, style = Stroke(1.5.dp.toPx()))
    }
}

// Thin slanted lines across the upper part.
private fun DrawScope.drawDiagonals(style: CoverStyle) {
    val area = size.height * 0.42f
    val step = 9.dp.toPx()
    clipRect(0f, 0f, size.width, area) {
        var x = -area
        while (x < size.width) {
            drawLine(style.band.copy(alpha = 0.55f), Offset(x, area), Offset(x + area, 0f), strokeWidth = 1.5.dp.toPx())
            x += step
        }
    }
}

// A round sun over a horizon line.
private fun DrawScope.drawSun(style: CoverStyle) {
    val horizon = size.height * 0.36f
    clipRect(0f, 0f, size.width, horizon) {
        drawCircle(style.band, radius = size.width * 0.24f, center = Offset(size.width * 0.5f, horizon))
    }
    drawLine(style.ink.copy(alpha = 0.55f), Offset(size.width * 0.12f, horizon), Offset(size.width * 0.88f, horizon), strokeWidth = 2.dp.toPx())
}

// A grid of dots in the upper part.
private fun DrawScope.drawDots(style: CoverStyle) {
    val step = 11.dp.toPx()
    val radius = 1.6.dp.toPx()
    var y = 16.dp.toPx()
    while (y < size.height * 0.38f) {
        var x = 12.dp.toPx()
        while (x < size.width - 6.dp.toPx()) {
            drawCircle(style.band.copy(alpha = 0.7f), radius, Offset(x, y))
            x += step
        }
        y += step
    }
}

// A tinted block across the top with a firm line under it.
private fun DrawScope.drawPanel(style: CoverStyle) {
    val height = size.height * 0.36f
    drawRect(style.band.copy(alpha = 0.28f), Offset.Zero, androidx.compose.ui.geometry.Size(size.width, height))
    drawRect(style.band, Offset(0f, height), androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx()))
}
