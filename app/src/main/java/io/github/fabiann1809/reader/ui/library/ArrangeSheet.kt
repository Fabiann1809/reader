package io.github.fabiann1809.reader.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookSort
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.prefs.LibraryLayout
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.ui.components.labelRes

/**
 * Order, filters and books per shelf: every change applies at once, so the shelves update behind
 * the sheet. The button at the bottom shows how many books the current filters leave.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArrangeSheet(
    arrangement: LibraryArrangement,
    layout: LibraryLayout,
    resultCount: Int,
    onChange: (LibraryArrangement) -> Unit,
    onLayoutChange: (LibraryLayout) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.library_arrange), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { onChange(arrangement.withoutFilters()) }, enabled = arrangement.hasFilters) {
                    Text(stringResource(R.string.arrange_clear), style = MaterialTheme.typography.labelLarge)
                }
            }
            SectionTitle(R.string.arrange_sort_title)
            BookSort.entries.forEach { sort ->
                SortRow(stringResource(sort.labelRes()), isSelected = arrangement.sort == sort) {
                    onChange(arrangement.copy(sort = sort))
                }
            }
            // Only the shelves use the count: the other views ignore it.
            if (layout.view == LibraryView.SHELVES) {
                SectionTitle(R.string.arrange_books_per_row)
                Chips {
                    LibraryLayout.BooksPerRowRange.forEach { count ->
                        ReaderFilterChip(
                            selected = layout.booksPerRow == count,
                            onClick = { onLayoutChange(layout.copy(booksPerRow = count)) },
                            label = { Text(count.toString()) },
                        )
                    }
                }
            }
            SectionTitle(R.string.arrange_status)
            Chips {
                BookStatus.entries.forEach { status ->
                    OptionChip(stringResource(status.labelRes()), isSelected = status in arrangement.statuses) {
                        onChange(arrangement.copy(statuses = arrangement.statuses.toggle(status)))
                    }
                }
            }
            SectionTitle(R.string.arrange_format)
            Chips {
                BookFormat.entries.forEach { format ->
                    OptionChip(stringResource(format.labelRes()), isSelected = format in arrangement.formats) {
                        onChange(arrangement.copy(formats = arrangement.formats.toggle(format)))
                    }
                }
            }
            SectionTitle(R.string.arrange_kind)
            Chips {
                BookKind.entries.forEach { kind ->
                    OptionChip(stringResource(kind.labelRes()), isSelected = kind in arrangement.kinds) {
                        onChange(arrangement.copy(kinds = arrangement.kinds.toggle(kind)))
                    }
                }
            }
            PrimaryButton(
                text = stringResource(
                    R.string.arrange_show,
                    pluralStringResource(R.plurals.library_book_count, resultCount, resultCount),
                ),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** One order option: its name and a ring that fills when it is the chosen one. */
@Composable
private fun SortRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.RadioButton, onClick = onClick),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Box(
            Modifier
                .size(22.dp)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        ) {
            if (isSelected) {
                Box(
                    Modifier
                        .size(22.dp)
                        .border(6.dp, MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}

// Selected chips show a check.
@Composable
private fun OptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    ReaderFilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (isSelected) {
            { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
    )
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

@StringRes
fun BookSort.labelRes(): Int = when (this) {
    BookSort.LAST_READ -> R.string.sort_last_read
    BookSort.TITLE -> R.string.sort_title
    BookSort.AUTHOR -> R.string.sort_author
    BookSort.DATE_ADDED -> R.string.sort_date_added
    BookSort.PROGRESS -> R.string.sort_progress
}

@StringRes
private fun BookFormat.labelRes(): Int = when (this) {
    BookFormat.EPUB -> R.string.format_epub
    BookFormat.PDF -> R.string.format_pdf
    BookFormat.TXT -> R.string.format_txt
    BookFormat.CBZ -> R.string.format_cbz
}

@StringRes
private fun BookKind.labelRes(): Int = when (this) {
    BookKind.DIGITAL -> R.string.kind_digital
    BookKind.PHYSICAL -> R.string.kind_physical
}

@StringRes
internal fun LibraryView.labelRes(): Int = when (this) {
    LibraryView.SHELVES -> R.string.view_shelves
    LibraryView.COLLECTIONS -> R.string.view_collections
    LibraryView.LIST -> R.string.view_list
}
