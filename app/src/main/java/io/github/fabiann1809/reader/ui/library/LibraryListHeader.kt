package io.github.fabiann1809.reader.ui.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.progressFraction
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion

/** What the library list starts with: "Sigues leyendo", then the sort label and the view switch. */
@Composable
fun LibraryListHeader(
    uiState: LibraryUiState,
    onContinue: (Long) -> Unit,
    onOpenArrange: () -> Unit,
    onViewChange: (LibraryView) -> Unit,
) {
    Column {
        val book = uiState.bookToContinue
        if (book != null && uiState.query.isBlank()) ContinueCard(book, onClick = { onContinue(book.id) })
        if (!uiState.libraryIsEmpty && !uiState.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            ) {
                SortLabel(uiState.arrangement.sort.labelRes(), onClick = onOpenArrange)
                ViewSwitch(selected = uiState.layout.view, onChange = onViewChange)
            }
        }
    }
}

@Composable
private fun ContinueCard(book: Book, onClick: () -> Unit) {
    val progress = book.progressFraction()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(
                onClickLabel = stringResource(R.string.library_continue_description, book.title),
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(10.dp),
        ) {
            BookCover(book = book, titleSize = 8.sp, modifier = Modifier.width(42.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_continue_reading),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (progress != null) ContinueProgress(progress)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_fill),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ContinueProgress(progress: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp),
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
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        Text(
            text = stringResource(R.string.library_continue_percent, (progress * PERCENT).toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SortLabel(label: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(R.drawable.ic_sort_descending), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private val ViewIcons = mapOf(
    LibraryView.SHELVES to R.drawable.ic_books,
    LibraryView.COLLECTIONS to R.drawable.ic_stack,
    LibraryView.LIST to R.drawable.ic_list_bullets,
)

private val ViewLabels = mapOf(
    LibraryView.SHELVES to R.string.view_shelves,
    LibraryView.COLLECTIONS to R.string.view_collections,
    LibraryView.LIST to R.string.view_list,
)

/** The three views in one pill: a thumb slides to the chosen one. */
@Composable
private fun ViewSwitch(selected: LibraryView, onChange: (LibraryView) -> Unit) {
    val views = LibraryView.entries
    val reduceMotion = rememberReduceMotion()
    val thumbOffset by animateDpAsState(
        targetValue = SwitchItemWidth * views.indexOf(selected),
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f),
        label = "viewThumb",
    )
    Box(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(SwitchItemWidth, SwitchItemHeight)
                .shadow(2.dp, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape),
        )
        Row {
            views.forEach { view ->
                val isSelected = view == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(SwitchItemWidth, SwitchItemHeight)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton, onClick = { onChange(view) }),
                ) {
                    Icon(
                        painter = painterResource(ViewIcons.getValue(view)),
                        contentDescription = stringResource(ViewLabels.getValue(view)),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

private val SwitchItemWidth = 44.dp
private val SwitchItemHeight = 38.dp
private const val PERCENT = 100
