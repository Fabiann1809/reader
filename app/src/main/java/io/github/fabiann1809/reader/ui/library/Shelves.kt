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
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.random.Random

// Shelf measurements from the design (04-system-design.md, section 6.2).
val ShelfSidePadding = 16.dp
val ShelfBookGap = 12.dp
private val SpaceAboveBooks = 20.dp
private val PlankTop = 8.dp
private val PlankFront = 8.dp
private val PlankShadow = 10.dp

// Books sink this much into the top surface of the plank so they look like they stand on it.
private val BookOverlap = 4.dp

/**
 * Wooden wall drawn in code (no bitmap, so no licensing issues): base color, vertical grain,
 * inner side shadows and a soft top/bottom vignette. The grain uses a fixed seed so it never flickers.
 */
fun Modifier.woodWall(): Modifier = composed {
    val base = ReaderTheme.colors.woodWall
    val grain = ReaderTheme.colors.woodGrain
    drawBehind {
        drawRect(base)
        val random = Random(GRAIN_SEED)
        var x = 0f
        while (x < size.width) {
            val stripe = (1 + random.nextInt(3)) * density
            drawRect(
                color = grain.copy(alpha = 0.12f + random.nextFloat() * 0.3f),
                topLeft = Offset(x, 0f),
                size = Size(stripe, size.height),
            )
            x += stripe + (4 + random.nextInt(10)) * density
        }
        val sideShadow = 24.dp.toPx()
        val shadow = Color.Black.copy(alpha = 0.25f)
        drawRect(
            brush = Brush.horizontalGradient(listOf(shadow, Color.Transparent), endX = sideShadow),
            size = Size(sideShadow, size.height),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, shadow),
                startX = size.width - sideShadow,
                endX = size.width,
            ),
            topLeft = Offset(size.width - sideShadow, 0f),
            size = Size(sideShadow, size.height),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.18f),
                0.12f to Color.Transparent,
                0.88f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.18f),
            ),
        )
    }
}

/**
 * One shelf: a row of up to [columns] slots standing on a full-bleed plank.
 * [slot] draws the item at each index (a cover, a placeholder or nothing).
 */
@Composable
fun Shelf(
    columns: Int,
    bookWidth: Dp,
    itemCount: Int,
    modifier: Modifier = Modifier,
    slot: @Composable (index: Int, width: Dp) -> Unit,
) {
    val bookHeight = bookWidth * 1.5f
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(top = SpaceAboveBooks + bookHeight - BookOverlap)) {
            Plank()
        }
        Row(
            modifier = Modifier.padding(top = SpaceAboveBooks, start = ShelfSidePadding, end = ShelfSidePadding),
            horizontalArrangement = Arrangement.spacedBy(ShelfBookGap),
        ) {
            repeat(minOf(itemCount, columns)) { index -> slot(index, bookWidth) }
        }
    }
}

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
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent))),
    )
}

/** Width of each book so that [columns] books and their gaps fill the shelf. */
fun shelfBookWidth(shelfWidth: Dp, columns: Int): Dp =
    (shelfWidth - ShelfSidePadding * 2 - ShelfBookGap * (columns - 1)) / columns

/** Full height of one shelf row for books of [bookWidth]. */
fun shelfRowHeight(bookWidth: Dp): Dp = SpaceAboveBooks + bookWidth * 1.5f - BookOverlap + PlankTop + PlankFront + PlankShadow

private const val GRAIN_SEED = 42
