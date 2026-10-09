package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.reader.TocEntry
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.util.formatDate
import kotlin.math.roundToInt

private const val TAB_INDEX = 0
private const val TAB_BOOKMARKS = 1

// Each level of the index goes this much further in.
private val LevelIndent = 16.dp

/** The reader's "Índice / Marcadores" (design 02 §2): a bottom sheet with a tab for each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentsSheet(
    state: ReaderUiState.Ready,
    onChapterClick: (TocEntry) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_INDEX) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                SheetTab(R.string.reader_index, selected = tab == TAB_INDEX) { tab = TAB_INDEX }
                SheetTab(R.string.reader_bookmarks, selected = tab == TAB_BOOKMARKS) { tab = TAB_BOOKMARKS }
            }
            when (tab) {
                TAB_INDEX -> TocList(state.tableOfContents, state.openChapter, onChapterClick)
                else -> BookmarkList(state.bookmarks, onBookmarkClick, onDeleteBookmark)
            }
        }
    }
}

// Only the chosen tab in the primary color, so it is clear which one is open.
@Composable
private fun SheetTab(label: Int, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = { Text(stringResource(label)) },
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TocList(entries: List<TocEntry>, openChapter: TocEntry?, onClick: (TocEntry) -> Unit) {
    if (entries.isEmpty()) {
        StatusMessage(icon = R.drawable.ic_list_bullets, title = stringResource(R.string.reader_toc_empty))
        return
    }
    // Opens on the chapter being read, so a long index doesn't start from the top.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = openChapter?.index ?: 0)
    LazyColumn(state = listState, contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 24.dp)) {
        items(entries, key = { it.index }) { entry ->
            val status = when {
                openChapter == null -> ChapterStatus.PENDING
                entry.index < openChapter.index -> ChapterStatus.DONE
                entry == openChapter -> ChapterStatus.CURRENT
                else -> ChapterStatus.PENDING
            }
            ChapterRow(entry, status) { onClick(entry) }
        }
    }
}

private enum class ChapterStatus { DONE, CURRENT, PENDING }

/** A chapter: read ones are checked, the open one is marked "Estás aquí", the rest wait with an empty circle. */
@Composable
private fun ChapterRow(entry: TocEntry, status: ChapterStatus, onClick: () -> Unit) {
    val isCurrent = status == ChapterStatus.CURRENT
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (status) {
                ChapterStatus.DONE -> Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = ReaderTheme.colors.success,
                    modifier = Modifier.size(20.dp),
                )
                ChapterStatus.CURRENT -> Icon(
                    painter = painterResource(R.drawable.ic_book_open),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                ChapterStatus.PENDING -> Box(
                    Modifier
                        .size(14.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = LevelIndent * entry.level)) {
            Text(
                text = entry.title ?: stringResource(R.string.reader_untitled_chapter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (status == ChapterStatus.PENDING) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.reader_toc_here),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun BookmarkList(bookmarks: List<Bookmark>, onClick: (Bookmark) -> Unit, onDelete: (Bookmark) -> Unit) {
    if (bookmarks.isEmpty()) {
        StatusMessage(
            icon = R.drawable.ic_bookmark_simple,
            title = stringResource(R.string.reader_bookmarks_empty_title),
            message = stringResource(R.string.reader_bookmarks_empty_hint),
        )
        return
    }
    LazyColumn {
        items(bookmarks, key = { it.id }) { bookmark ->
            ListItem(
                headlineContent = { Text(bookmark.chapter ?: stringResource(R.string.reader_bookmark), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    val percent = ((bookmark.progression ?: 0.0) * 100).roundToInt()
                    Text(stringResource(R.string.reader_bookmark_detail, percent, formatDate(bookmark.createdAt)))
                },
                leadingContent = {
                    Icon(painterResource(R.drawable.ic_bookmark_simple_fill), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    IconButton(onClick = { onDelete(bookmark) }) {
                        Icon(painterResource(R.drawable.ic_trash), contentDescription = stringResource(R.string.reader_bookmark_delete))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.clickable(role = Role.Button) { onClick(bookmark) },
            )
        }
    }
}
