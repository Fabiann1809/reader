package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.prefs.LibraryView
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.shelfCoverAspect
import io.github.fabiann1809.reader.ui.components.StatusMessage

/** How books react to touch: a tap and a long press, each receiving the book's id. */
class BookGestures(val onClick: (Long) -> Unit, val onLongClick: (Long) -> Unit)

/**
 * The books area: placeholders while loading, the empty state when there is nothing to show,
 * or the books in the chosen view ((shelves, collections or list)).
 */
@Composable
fun LibraryBooks(
    uiState: LibraryUiState,
    contentPadding: PaddingValues,
    empty: EmptyState?,
    gestures: BookGestures,
    bookFrame: BookFrame,
    header: (@Composable () -> Unit)? = null,
    collectionsActions: CollectionsActions = CollectionsActions(),
) {
    val isShelves = uiState.layout.view == LibraryView.SHELVES
    // The Colecciones view lists every collection, so what the shown one lacks does not make it empty.
    val empty = if (uiState.layout.view == LibraryView.COLLECTIONS && !uiState.libraryIsEmpty) null else empty
    val columns = booksPerRow(uiState.layout.booksPerRow)
    when {
        uiState.isLoading && isShelves -> Bookcase(contentPadding, columns, itemCount = PLACEHOLDER_COUNT) { _, width ->
            PlaceholderCover(width)
        }
        uiState.isLoading -> Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        empty != null && isShelves -> EmptyShelf(contentPadding, empty)
        empty != null -> Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            StatusMessage(
                icon = empty.icon,
                title = empty.title,
                message = empty.message,
                action = { PrimaryButton(text = empty.action, onClick = empty.onAction, icon = empty.actionIcon) },
            )
        }
        else -> when (uiState.layout.view) {
            LibraryView.SHELVES -> Bookcase(
                contentPadding,
                columns,
                itemCount = uiState.books.size,
                bottomSpace = FAB_SPACE,
                header = header,
            ) { index, width ->
                val book = uiState.books[index]
                bookFrame(book, Modifier.shelfTilt(book.title)) {
                    BookCover(
                        book = book,
                        onClick = { gestures.onClick(book.id) },
                        onLongClick = { gestures.onLongClick(book.id) },
                        showBadges = true,
                        aspect = shelfCoverAspect(book.title),
                        modifier = Modifier.width(width),
                    )
                }
            }
            LibraryView.COLLECTIONS ->
                CollectionsView(uiState.collectionGroups, contentPadding, FAB_SPACE, collectionsActions, header)
            LibraryView.LIST ->
                BookList(uiState.books, contentPadding, FAB_SPACE, gestures.onClick, gestures.onLongClick, bookFrame, header)
        }
    }
}

/**
 * Wraps each book with its long-press menu, the selection look and the rising appearance.
 * [menuBookId] is the book whose menu is open.
 */
@Composable
fun rememberBookFrame(
    uiState: LibraryUiState,
    menuBookId: Long?,
    onMenuDismiss: () -> Unit,
    menuActions: BookMenuActions,
): BookFrame {
    val appearance = rememberBookAppearance(hasBooks = !uiState.isLoading && uiState.books.isNotEmpty())
    val positions = remember(uiState.books) { uiState.books.withIndex().associate { (index, book) -> book.id to index } }
    return { book: Book, modifier: Modifier, content: @Composable () -> Unit ->
        SelectionFrame(
            isSelected = if (uiState.isSelecting) book.id in uiState.selectedIds else null,
            modifier = modifier.bookAppearance(appearance, positions[book.id] ?: 0),
        ) {
            content()
            BookMenu(book, expanded = menuBookId == book.id, onDismiss = onMenuDismiss, actions = menuActions)
        }
    }
}

/** The user's books per row, capped at 2 when the system font is scaled up a lot so titles stay readable. */
@Composable
private fun booksPerRow(chosen: Int): Int = if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) minOf(chosen, 2) else chosen

private const val PLACEHOLDER_COUNT = 6
private const val LARGE_FONT_SCALE = 1.3f

// Keeps the last shelf visible above the floating button.
private val FAB_SPACE = 88.dp
