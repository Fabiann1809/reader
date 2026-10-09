package io.github.fabiann1809.reader.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.isNew
import io.github.fabiann1809.reader.ui.theme.Fraunces
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.roundToInt

// Covers are 2:3 with a tighter radius on the spine side (design 6.4).
private val CoverShape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 4.dp, bottomEnd = 4.dp)

/**
 * A book's cover: its stored cover image when it has one (imported books), otherwise a generated one
 * with a muted color picked from the title, the title in serif and the author. Both get a thin
 * progress bar at the bottom when progress is known.
 */
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 15.sp,
    onClick: (() -> Unit)? = null,
    // Long-press opens the book's menu in the library (design 6.4).
    onLongClick: (() -> Unit)? = null,
    // "Nuevo" and "Físico" badges; only the library shows them.
    showBadges: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress = book.progressFraction()
    val coverImage = rememberCoverImage(book.coverPath)
    val baseDescription = coverDescription(book, progress)
    // The badges are drawn inside the cleared semantics below, so screen readers hear them here.
    val badges = listOfNotNull(
        stringResource(R.string.book_badge_new).takeIf { showBadges && book.isNew() },
        stringResource(R.string.kind_physical).takeIf { showBadges && book.kind == BookKind.PHYSICAL },
    )
    val description = (listOf(baseDescription) + badges).joinToString(", ")

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .scale(if (pressed) 0.97f else 1f)
            .shadow(elevation = if (pressed) 1.dp else 3.dp, shape = CoverShape)
            .clip(CoverShape)
            .background(coverColor(book.title))
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onLongClickLabel = onLongClick?.let { stringResource(R.string.book_menu) },
                        onLongClick = onLongClick,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            },
    ) {
        // The real cover replaces the generated one; the gloss, spine and progress stay on top of both.
        if (coverImage != null) {
            Image(
                bitmap = coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
        // A real cover already shows its title and author.
        if (coverImage == null) {
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
        }
        if (showBadges) {
            CoverBadges(book, Modifier.align(Alignment.TopEnd))
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

/** Title, author and progress for screen readers; books imported without metadata have no author. */
@Composable
private fun coverDescription(book: Book, progress: Float?): String {
    val hasAuthor = book.author.isNotBlank()
    if (progress == null) {
        return if (hasAuthor) stringResource(R.string.book_cover_description, book.title, book.author) else book.title
    }
    val percent = (progress * 100).roundToInt()
    return if (hasAuthor) {
        stringResource(R.string.book_cover_description_progress, book.title, book.author, percent)
    } else {
        stringResource(R.string.book_cover_description_progress_no_author, book.title, percent)
    }
}

/** "Nuevo" pill and "Físico" hand in the top corner (design 6.4). They are visual only: the cover's description covers them. */
@Composable
private fun CoverBadges(book: Book, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (book.isNew()) {
            Text(
                text = stringResource(R.string.book_badge_new),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (book.kind == BookKind.PHYSICAL) {
            Icon(
                painter = painterResource(R.drawable.ic_hand),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .padding(3.dp)
                    .size(12.dp),
            )
        }
    }
}
