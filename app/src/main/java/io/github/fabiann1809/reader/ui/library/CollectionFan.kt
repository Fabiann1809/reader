package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private val CoverWidth = 86.dp
private val CoverHeight = 128.dp
private val BandTop = 66.dp
private val BandHeight = 118.dp
private const val FAN_SIZE = 5
private const val FAN_SPREAD_DP = 50
private const val FAN_TILT_DEGREES = 4f
private const val FAN_RISE_DP = 7
private const val BAND_Z = 20f

/**
 * Up to five covers in a fan behind a frosted pastel band of [colorIndex]. [onBookClick] makes the
 * covers tappable, [bandContent] fills the right side of the band and [emptyPlaceholder] draws a
 * dashed ghost cover while there are no books.
 */
@Composable
fun CollectionFanArt(
    books: List<Book>,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    onBookClick: ((Long) -> Unit)? = null,
    emptyPlaceholder: Boolean = false,
    bandContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(BandTop + BandHeight + 10.dp)) {
        if (books.isEmpty() && emptyPlaceholder) GhostCover(width = maxWidth)
        CoverFan(books, onBookClick, maxWidth)
        Band(colorIndex, bandContent)
    }
}

@Composable
private fun GhostCover(width: Dp) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(x = width / 2 - CoverWidth / 2, y = 6.dp)
            .size(CoverWidth, CoverHeight)
            .drawBehind {
                drawRoundRect(
                    color = line,
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
                )
            },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_books),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun CoverFan(books: List<Book>, onBookClick: ((Long) -> Unit)?, width: Dp) {
    val covers = books.take(FAN_SIZE)
    val center = (covers.size - 1) / 2f
    covers.forEachIndexed { i, book ->
        val d = i - center
        BookCover(
            book = book,
            titleSize = 15.sp,
            aspect = CoverWidth / CoverHeight,
            onClick = onBookClick?.let { click -> { click(book.id) } },
            modifier = Modifier
                .offset(x = width / 2 - CoverWidth / 2 + (d * FAN_SPREAD_DP).dp, y = (abs(d) * FAN_RISE_DP).dp)
                .zIndex(FAN_SIZE * 2f - (abs(d) * 2).roundToInt())
                .rotate(d * FAN_TILT_DEGREES)
                .width(CoverWidth),
        )
    }
}

@Composable
private fun Band(colorIndex: Int, content: (@Composable ColumnScope.() -> Unit)?) {
    val pastels = ReaderTheme.colors.pastels
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .offset(y = BandTop)
            .zIndex(BAND_Z)
            .fillMaxWidth()
            .height(BandHeight)
            .clip(shape)
            .background(Brush.horizontalGradient(pastels[colorIndex.mod(pastels.size)])),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.44f)
                .background(Color.White.copy(alpha = 0.2f)),
        )
        content?.let {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 11.dp, end = 14.dp),
                content = it,
            )
        }
    }
}

/** Round white button on a band. */
@Composable
fun BandButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = BandIcon, modifier = Modifier.size(20.dp))
    }
}

private val BandIcon = Color(0xFF241B15)
