package io.github.fabiann1809.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.theme.Fraunces
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.roundToInt

// Covers are 2:3 with a tighter radius on the spine side (design 6.4).
private val CoverShape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 4.dp, bottomEnd = 4.dp)

/**
 * Generated cover for a book (the app has no cover images): a muted color picked from the title,
 * the title in serif, the author, and a thin progress bar at the bottom when progress is known.
 */
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 15.sp,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress = book.progressFraction()
    val description = if (progress == null) {
        stringResource(R.string.book_cover_description, book.title, book.author)
    } else {
        stringResource(R.string.book_cover_description_progress, book.title, book.author, (progress * 100).roundToInt())
    }

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .scale(if (pressed) 0.97f else 1f)
            .shadow(elevation = if (pressed) 1.dp else 3.dp, shape = CoverShape)
            .clip(CoverShape)
            .background(coverColor(book.title))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            },
    ) {
        // Subtle diagonal gloss.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent))),
        )
        // Spine highlight on the left edge.
        Box(
            Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 8.dp, bottom = 12.dp),
        ) {
            Text(
                text = book.title,
                color = Color.White,
                fontFamily = Fraunces,
                fontSize = titleSize,
                lineHeight = titleSize * 1.15f,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = book.author,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        progress?.let { fraction ->
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.3f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(ReaderTheme.colors.progress),
                )
            }
        }
    }
}

/** Same title, same color: a stable hash so a book keeps its color across launches. */
@Composable
private fun coverColor(title: String): Color {
    val palette = ReaderTheme.colors.covers
    return palette[Math.floorMod(title.hashCode(), palette.size)]
}
