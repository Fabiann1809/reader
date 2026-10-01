package io.github.fabiann1809.reader.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookSort
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.ui.components.labelRes

/** "Ordenar y filtrar": every change applies at once, so the shelves update behind the sheet. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArrangeSheet(
    arrangement: LibraryArrangement,
    onChange: (LibraryArrangement) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.library_arrange), style = MaterialTheme.typography.titleMedium)

            Section(R.string.arrange_sort_title) {
                BookSort.entries.forEach { sort ->
                    OptionChip(sort.labelRes(), isSelected = arrangement.sort == sort) {
                        onChange(arrangement.copy(sort = sort))
                    }
                }
            }
            Section(R.string.arrange_status) {
                BookStatus.entries.forEach { status ->
                    OptionChip(status.labelRes(), isSelected = status in arrangement.statuses) {
                        onChange(arrangement.copy(statuses = arrangement.statuses.toggle(status)))
                    }
                }
            }
            Section(R.string.arrange_format) {
                BookFormat.entries.forEach { format ->
                    OptionChip(format.labelRes(), isSelected = format in arrangement.formats) {
                        onChange(arrangement.copy(formats = arrangement.formats.toggle(format)))
                    }
                }
            }
            Section(R.string.arrange_kind) {
                BookKind.entries.forEach { kind ->
                    OptionChip(kind.labelRes(), isSelected = kind in arrangement.kinds) {
                        onChange(arrangement.copy(kinds = arrangement.kinds.toggle(kind)))
                    }
                }
            }
            TextButton(onClick = { onChange(arrangement.withoutFilters()) }, enabled = arrangement.hasFilters) {
                Text(stringResource(R.string.arrange_clear_filters))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(@StringRes title: Int, chips: @Composable () -> Unit) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
}

// Design 7.5: selected chips use primary-95 with a check.
@Composable
private fun OptionChip(@StringRes label: Int, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(stringResource(label)) },
        leadingIcon = if (isSelected) {
            { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

@StringRes
private fun BookSort.labelRes(): Int = when (this) {
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
