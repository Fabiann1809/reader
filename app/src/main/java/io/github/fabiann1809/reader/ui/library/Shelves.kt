package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

// Shelf measurements of the redesign prototype.
val ShelfSidePadding = 16.dp
private val SpaceAboveBooks = 26.dp
private val PlankTop = 9.dp
private val PlankFront = 22.dp
private val PlankShadow = 10.dp
private val MinBookGap = 8.dp
private val WallEdge = 30.dp

// The tallest generated cover is 1.62 times its width; the shelf leaves room for it.
private const val TALLEST_BOOK = 1.62f

// With three books per shelf, a decoration takes the fourth place (as in the prototype).
private const val DECORATED_COLUMNS = 3

/** Flat wall of the library: the wall color with a darker strip at each edge. */
fun Modifier.libraryWall(): Modifier = composed {
    val colors = ReaderTheme.colors
    drawBehind {
        val edge = WallEdge.toPx()
        drawRect(colors.woodWall)
        drawRect(colors.woodGrain, Offset.Zero, Size(edge, size.height))
        drawRect(colors.woodGrain, Offset(size.width - edge, 0f), Size(edge, size.height))
    }
}

/**
 * One shelf: a row of up to [columns] slots standing on a full-bleed plank, with a decoration
 * when [columns] is three. [slot] draws the item at each index (a cover, a placeholder or nothing).
 */
@Composable
fun Shelf(
    columns: Int,
    bookWidth: Dp,
    itemCount: Int,
    modifier: Modifier = Modifier,
    shelfIndex: Int = 0,
    slot: @Composable (index: Int, width: Dp) -> Unit,
) {
    val decor = ShelfDecor.entries[shelfIndex % ShelfDecor.entries.size].takeIf { columns == DECORATED_COLUMNS }
    Column(modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .libraryWall()
                .height(SpaceAboveBooks + maxOf(bookWidth * TALLEST_BOOK, ShelfDecor.VASE.height))
                .padding(top = SpaceAboveBooks, start = ShelfSidePadding, end = ShelfSidePadding),
        ) {
            // The vase stands first on its shelf, the other decorations last.
            if (decor == ShelfDecor.VASE) ShelfDecorItem(decor, delayMillis = DECOR_DELAY_MILLIS)
            repeat(minOf(itemCount, columns)) { index -> slot(index, bookWidth) }
            if (decor != null && decor != ShelfDecor.VASE) ShelfDecorItem(decor, delayMillis = DECOR_DELAY_MILLIS)
        }
        Plank()
    }
}

private const val DECOR_DELAY_MILLIS = 260

@Composable
private fun Plank() {
    val colors = ReaderTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(PlankTop)
            .background(Brush.verticalGradient(colors.shelfTop)),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(PlankFront)
            .background(Brush.verticalGradient(colors.shelfFront)),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(PlankShadow)
            .libraryWall()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent))),
    )
}

/** Width of each book so that [columns] books (and the decoration, when there is one) fill the shelf. */
fun shelfBookWidth(shelfWidth: Dp, columns: Int): Dp {
    val decor = if (columns == DECORATED_COLUMNS) ShelfDecorSlot + MinBookGap else 0.dp
    return (shelfWidth - ShelfSidePadding * 2 - decor - MinBookGap * (columns - 1)) / columns
}

/** A few books lean slightly on their shelf; which ones is fixed by the title. */
fun Modifier.shelfTilt(title: String): Modifier =
    if (Math.floorMod(title.hashCode(), TILT_EVERY) == TILT_REMAINDER) {
        graphicsLayer {
            rotationZ = -TILT_DEGREES
            transformOrigin = TransformOrigin(0f, 1f)
        }
    } else {
        this
    }

private const val TILT_EVERY = 5
private const val TILT_REMAINDER = 2
private const val TILT_DEGREES = 3f
