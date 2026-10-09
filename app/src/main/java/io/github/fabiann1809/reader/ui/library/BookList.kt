package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.progressFraction

/** "Lista": one card per book with a small cover, title, author, format and a progress bar. */
@Composable
fun BookList(
    books: List<Book>,
    contentPadding: PaddingValues,
    bottomSpace: Dp,
    onBookClick: (Long) -> Unit,
    onBookLongClick: (Long) -> Unit,
    bookFrame: BookFrame,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + bottomSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        header?.let { item { it() } }
        items(books, key = { it.id }) { book ->
            bookFrame(book, Modifier.padding(horizontal = 20.dp)) {
                BookRow(book = book, onClick = { onBookClick(book.id) }, onLongClick = { onBookLongClick(book.id) })
            }
        }
    }
}

@Composable
private fun BookRow(book: Book, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                role = Role.Button,
                onLongClickLabel = stringResource(R.string.book_menu),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        ) {
            BookCover(book = book, titleSize = 7.sp, modifier = Modifier.width(LIST_COVER_WIDTH))
            Column(Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.book_author_format, book.author, formatLabel(book)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                RowProgress(book)
            }
            MenuButton(onClick = onLongClick)
        }
    }
}

@Composable
private fun RowProgress(book: Book) {
    val fraction = if (book.status == BookStatus.FINISHED) 1f else book.progressFraction() ?: 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        Text(
            text = progressLabel(fraction),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(PROGRESS_LABEL_WIDTH),
        )
    }
}

@Composable
private fun progressLabel(fraction: Float): String = when {
    fraction >= 1f -> stringResource(R.string.book_progress_finished)
    fraction <= 0f -> stringResource(R.string.book_progress_not_started)
    else -> stringResource(R.string.library_continue_percent, (fraction * PERCENT).toInt())
}

@Composable
private fun formatLabel(book: Book): String =
    if (book.kind == BookKind.PHYSICAL) stringResource(R.string.kind_physical) else book.format?.name.orEmpty()

@Composable
private fun MenuButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.book_menu), onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dots_three_vertical),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private val LIST_COVER_WIDTH = 48.dp
private val PROGRESS_LABEL_WIDTH = 92.dp
private const val PERCENT = 100
