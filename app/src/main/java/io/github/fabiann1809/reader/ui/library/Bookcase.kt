package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import kotlin.math.ceil

/**
 * Shelves filled with [itemCount] items, [columns] per shelf, on the library wall. The optional
 * [header] comes first, on the plain background.
 */
@Composable
fun Bookcase(
    contentPadding: PaddingValues,
    columns: Int,
    itemCount: Int,
    bottomSpace: Dp = 0.dp,
    header: (@Composable () -> Unit)? = null,
    slot: @Composable (index: Int, width: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val bookWidth = shelfBookWidth(maxWidth, columns)
        val shelves = ceil(itemCount / columns.toFloat()).toInt()
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + bottomSpace,
            ),
        ) {
            header?.let { item { it() } }
            items(count = shelves) { shelf ->
                val first = shelf * columns
                Shelf(columns = columns, bookWidth = bookWidth, itemCount = itemCount - first, shelfIndex = shelf) { i, width ->
                    slot(first + i, width)
                }
            }
            item { Box(Modifier.fillMaxWidth().height(WALL_FOOT).libraryWall()) }
        }
    }
}

private val WALL_FOOT = 30.dp

/** Light block standing in for a cover while the library loads. */
@Composable
fun PlaceholderCover(width: Dp) {
    Box(
        Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(4.dp)),
    )
}

/** Shelf with a dashed "ghost" book (design 6.6), a message and one action. */
@Composable
fun EmptyShelf(contentPadding: PaddingValues, empty: EmptyState) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val bookWidth = shelfBookWidth(maxWidth, columns = 3)
        Column {
            Shelf(columns = 3, bookWidth = bookWidth, itemCount = 1) { _, width -> GhostBook(width) }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = empty.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = empty.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = empty.action,
                    onClick = empty.onAction,
                    icon = empty.actionIcon,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Dashed outline of a book: the empty-state placeholder from the design. */
@Composable
private fun GhostBook(width: Dp) {
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_book),
            contentDescription = null,
            tint = outline,
            modifier = Modifier.size(32.dp),
        )
    }
}
