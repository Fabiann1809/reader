package io.github.fabiann1809.reader.ui.library

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.LibraryArrangement
import io.github.fabiann1809.reader.data.collection.LibraryFilter

/** Texts and action of an empty library, collection, search or filter result. */
class EmptyState(
    val title: String,
    val message: String,
    val action: String,
    @DrawableRes val actionIcon: Int,
    val onAction: () -> Unit,
    @DrawableRes val icon: Int = R.drawable.ic_books,
)

/** Null when there are books to show. */
@Composable
fun emptyState(
    uiState: LibraryUiState,
    onAddBook: () -> Unit,
    onSearch: (String) -> Unit,
    onArrangementChange: (LibraryArrangement) -> Unit,
    onSelectFilter: (LibraryFilter) -> Unit,
): EmptyState? = when {
    uiState.libraryIsEmpty -> EmptyState(
        title = stringResource(R.string.library_empty_title),
        message = stringResource(R.string.library_empty_message),
        action = stringResource(R.string.library_add_book),
        actionIcon = R.drawable.ic_plus_circle,
        onAction = onAddBook,
    )
    uiState.books.isNotEmpty() -> null
    // The search is cleared first; if the filters still hide everything, this shows again for them.
    uiState.query.isNotBlank() -> EmptyState(
        title = stringResource(R.string.library_search_empty_title),
        message = stringResource(R.string.library_search_empty_message, uiState.query.trim()),
        action = stringResource(R.string.library_search_clear),
        actionIcon = R.drawable.ic_x,
        onAction = { onSearch("") },
        icon = R.drawable.ic_magnifying_glass,
    )
    uiState.arrangement.hasFilters -> EmptyState(
        title = stringResource(R.string.library_search_empty_title),
        message = stringResource(R.string.library_filters_empty_message),
        action = stringResource(R.string.arrange_clear_filters),
        actionIcon = R.drawable.ic_x,
        onAction = { onArrangementChange(uiState.arrangement.withoutFilters()) },
        icon = R.drawable.ic_funnel_simple,
    )
    else -> EmptyState(
        title = stringResource(R.string.collection_empty_title),
        message = stringResource(
            if (uiState.currentCollection != null) R.string.collection_empty_message else R.string.collection_smart_empty_message,
        ),
        action = stringResource(R.string.collection_show_all),
        actionIcon = R.drawable.ic_books,
        onAction = { onSelectFilter(LibraryFilter.Default) },
    )
}
