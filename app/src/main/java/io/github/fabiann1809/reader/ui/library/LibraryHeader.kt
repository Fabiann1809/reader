package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip

// Chips of the header, in the prototype order.
private val ChipCollections = listOf(
    SmartCollection.ALL,
    SmartCollection.READING,
    SmartCollection.TO_READ,
    SmartCollection.FINISHED,
    SmartCollection.FAVORITES,
)

/** Title with the collection selector and book count, search and filter buttons, and the collection chips. */
@Composable
fun LibraryHeader(
    uiState: LibraryUiState,
    onOpenSearch: () -> Unit,
    onOpenDialog: (LibraryDialog) -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
) {
    Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            TitleBlock(uiState, onClick = { onOpenDialog(LibraryDialog.PICKER) }, modifier = Modifier.weight(1f))
            if (!uiState.libraryIsEmpty) {
                // Only collections created by the user can be renamed or deleted.
                if (uiState.currentCollection != null) {
                    CollectionMenu(
                        onRename = { onOpenDialog(LibraryDialog.RENAME) },
                        onDelete = { onOpenDialog(LibraryDialog.DELETE) },
                    )
                }
                HeaderButton(R.drawable.ic_magnifying_glass, R.string.library_search, onOpenSearch)
                HeaderButton(
                    icon = R.drawable.ic_sliders_horizontal,
                    description = R.string.library_arrange,
                    onClick = { onOpenDialog(LibraryDialog.ARRANGE) },
                    showDot = uiState.arrangement.hasFilters,
                )
            }
        }
        if (!uiState.libraryIsEmpty) CollectionChips(uiState, onSelectFilter)
    }
}

@Composable
private fun TitleBlock(uiState: LibraryUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = uiState.currentCollection?.name
        ?: stringResource((uiState.filter as? LibraryFilter.Smart)?.collection?.nameRes() ?: R.string.collection_all)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(
                onClickLabel = stringResource(R.string.collection_change),
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(painterResource(R.drawable.ic_caret_down), contentDescription = null, modifier = Modifier.size(20.dp))
        }
        if (!uiState.isLoading) {
            Text(
                text = pluralStringResource(R.plurals.library_book_count, uiState.books.size, uiState.books.size),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HeaderButton(icon: Int, description: Int, onClick: () -> Unit, showDot: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = stringResource(description), modifier = Modifier.size(22.dp))
        if (showDot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(9.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun CollectionChips(uiState: LibraryUiState, onSelectFilter: (LibraryFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        ChipCollections.forEach { collection ->
            val count = uiState.smartCounts[collection] ?: 0
            ReaderFilterChip(
                selected = uiState.filter == LibraryFilter.Smart(collection),
                onClick = { onSelectFilter(LibraryFilter.Smart(collection)) },
                label = { Text(stringResource(R.string.library_chip, stringResource(collection.chipRes()), count)) },
            )
        }
    }
}

private fun SmartCollection.chipRes(): Int = when (this) {
    SmartCollection.FAVORITES -> R.string.library_chip_favorites
    SmartCollection.READING -> R.string.library_chip_reading
    else -> nameRes()
}
