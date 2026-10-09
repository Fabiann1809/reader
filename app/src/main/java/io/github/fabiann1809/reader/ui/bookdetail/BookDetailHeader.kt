package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.coverStyle
import io.github.fabiann1809.reader.ui.components.labelRes
import io.github.fabiann1809.reader.ui.components.progressFraction
import io.github.fabiann1809.reader.ui.components.rememberEntranceProgress
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import kotlin.math.roundToInt

private val CoverWidth = 140.dp
private val CoverHeight = 208.dp
private val PaperColor = Color(0xFFFBF7F0)
private val PaperLine = Color(0xFFE3D6C4)
private const val COVER_TURN_DEGREES = 40f

/**
 * The top of the detail: a wash of the cover's color behind the back, favorite and menu buttons, the
 * cover opening over a sheet of paper, the title and who wrote it.
 */
@Composable
fun DetailHero(
    book: Book,
    onNavigateUp: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    menu: @Composable () -> Unit,
) {
    val wash = coverStyle(book.title).cover.copy(alpha = 0.18f)
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = 60.dp)
                .background(wash),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                HeroButton(R.drawable.ic_arrow_left, stringResource(R.string.navigate_up), onNavigateUp)
                Box(Modifier.weight(1f))
                FavoriteButton(book.isFavorite, onFavoriteChange)
                menu()
            }
            OpeningCover(book)
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp),
            )
            Text(
                text = subtitle(book),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun subtitle(book: Book): String {
    val format = if (book.kind == BookKind.PHYSICAL) stringResource(R.string.kind_physical) else book.format?.name.orEmpty()
    val pages = book.totalPages?.let { stringResource(R.string.detail_pages_short, it) }
    return listOf(book.author, format, pages).filter { !it.isNullOrBlank() }.joinToString(" · ")
}

@Composable
private fun HeroButton(icon: Int, description: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onChange: (Boolean) -> Unit) {
    HeroButton(
        icon = if (isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart,
        description = stringResource(if (isFavorite) R.string.favorite_remove else R.string.favorite_add),
        onClick = { onChange(!isFavorite) },
        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

/** The cover turns open on its spine when the screen appears, over a sheet of lined paper. */
@Composable
private fun OpeningCover(book: Book) {
    val progress = rememberEntranceProgress(durationMillis = 600, easing = Motion.PopEasing)
    val density = LocalDensity.current.density
    Box(
        Modifier
            .size(CoverWidth, CoverHeight)
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
                cameraDistance = 12f * density
                rotationY = -COVER_TURN_DEGREES * (1f - progress)
                scaleX = 0.7f + 0.3f * progress
                scaleY = 0.7f + 0.3f * progress
                translationY = (1f - progress) * 30.dp.toPx()
            },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .matchParentSize()
                .padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                .background(PaperColor, RoundedCornerShape(topStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 2.dp))
                .padding(start = 22.dp, end = 16.dp, top = 28.dp),
        ) {
            listOf(1f, 1f, 0.8f, 1f, 0.6f).forEach { fraction ->
                Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(PaperLine, RoundedCornerShape(2.dp)))
            }
        }
        BookCover(book = book, titleSize = 24.sp, modifier = Modifier.matchParentSize().offset(x = (-3).dp))
    }
}

/** Big percentage, the page line, the bar, and what is left to read. Tap to update the page. */
@Composable
fun ProgressCard(book: Book, remaining: RemainingReading?, onClick: () -> Unit) {
    val fraction = book.progressFraction()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClickLabel = stringResource(R.string.update_progress_title), role = Role.Button, onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = fraction?.let { stringResource(R.string.book_progress_percent, (it * 100).roundToInt()) }
                        ?: stringResource(R.string.book_progress_not_started),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                )
                Text(
                    text = bookProgressLine(book),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            fraction?.let {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 12.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Box(Modifier.fillMaxWidth(it).fillMaxHeight().background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
            remaining?.let { RemainingFacts(it) }
        }
    }
}

@Composable
private fun bookProgressLine(book: Book): String = when (val total = book.totalPages) {
    null -> stringResource(R.string.book_progress, book.currentPage)
    else -> stringResource(R.string.book_progress_with_total, book.currentPage, total)
}

@Composable
private fun RemainingFacts(remaining: RemainingReading) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Fact(R.drawable.ic_book_open, pluralStringResource(R.plurals.detail_pages_left, remaining.pages, remaining.pages))
        val minutes = remaining.minutes
        if (minutes != null && remaining.pages > 0) Fact(R.drawable.ic_clock, durationText(minutes))
    }
}

@Composable
private fun Fact(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Por leer · Leyendo · Terminado in one pill, with a thumb that slides to the current one. */
@Composable
fun StatusSwitch(status: BookStatus, onChange: (BookStatus) -> Unit) {
    val statuses = BookStatus.entries
    val reduceMotion = rememberReduceMotion()
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        val itemWidth = (maxWidth - 8.dp) / statuses.size
        val thumbOffset by animateDpAsState(
            targetValue = itemWidth * statuses.indexOf(status),
            animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f),
            label = "statusThumb",
        )
        Box(
            Modifier
                .padding(4.dp)
                .offset(x = thumbOffset)
                .size(itemWidth, 40.dp)
                .shadow(2.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
        )
        Row(Modifier.padding(4.dp)) {
            statuses.forEach { option ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(itemWidth)
                        .height(40.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) { onChange(option) },
                ) {
                    Text(
                        text = stringResource(option.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (option == status) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The main action and the round buttons for capturing a page and filing the book in a collection. */
@Composable
fun DetailActionRow(
    book: Book,
    onRead: () -> Unit,
    onCapturePage: () -> Unit,
    onCollection: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        val isDigital = book.kind == BookKind.DIGITAL
        PrimaryAction(
            text = stringResource(
                when {
                    !isDigital -> R.string.capture_title
                    book.lastOpenedAt == null -> R.string.book_read
                    else -> R.string.detail_continue_reading
                },
            ),
            icon = if (isDigital) R.drawable.ic_book_open_text else R.drawable.ic_camera,
            onClick = if (isDigital) onRead else onCapturePage,
            modifier = Modifier.weight(1f),
        )
        // A paper book already has the camera as its main action.
        if (isDigital) OutlineCircle(R.drawable.ic_camera, stringResource(R.string.capture_title), onCapturePage)
        OutlineCircle(R.drawable.ic_books, stringResource(R.string.collection_add_title), onCollection)
    }
}

@Composable
private fun PrimaryAction(text: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .height(54.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun OutlineCircle(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(22.dp))
    }
}
