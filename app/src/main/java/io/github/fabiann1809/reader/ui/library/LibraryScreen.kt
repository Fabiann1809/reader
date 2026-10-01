package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.LightStatusBarIcons
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.Primary40
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.ceil

@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LightStatusBarIcons()
    LibraryContent(
        uiState = uiState,
        onBookClick = onBookClick,
        onAddBook = onAddBook,
    )
}

@Composable
fun LibraryContent(
    uiState: LibraryUiState,
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showFab = !uiState.isLoading && uiState.books.isNotEmpty()
    Scaffold(
        modifier = modifier.woodWall(),
        containerColor = Color.Transparent,
        topBar = {
            // The library bar stays forest green in both themes: it is the app's identity.
            ReaderTopAppBar(
                title = stringResource(R.string.library_title),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary40,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            if (showFab) {
                // Content overload on purpose: the text/icon overload hides the label from screen readers.
                ExtendedFloatingActionButton(
                    onClick = onAddBook,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(painterResource(R.drawable.ic_plus_circle), contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.library_add_book))
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Bookcase(innerPadding, itemCount = PLACEHOLDER_COUNT) { _, width -> PlaceholderCover(width) }
            uiState.books.isEmpty() -> EmptyLibrary(innerPadding, onAddBook)
            else -> Bookcase(innerPadding, itemCount = uiState.books.size, bottomSpace = FAB_SPACE) { index, width ->
                val book = uiState.books[index]
                BookCover(book = book, onClick = { onBookClick(book.id) }, modifier = Modifier.width(width))
            }
        }
    }
}

/**
 * Shelves filled with [itemCount] items, [columns] per shelf. Extra empty shelves are added
 * so the wall is always covered with shelves down to the bottom of the screen.
 */
@Composable
private fun Bookcase(
    contentPadding: PaddingValues,
    itemCount: Int,
    bottomSpace: Dp = 0.dp,
    slot: @Composable (index: Int, width: Dp) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = shelfColumns()
        val bookWidth = shelfBookWidth(maxWidth, columns)
        val rowHeight = shelfRowHeight(bookWidth)
        val shelvesWithBooks = ceil(itemCount / columns.toFloat()).toInt()
        val shelvesToFillScreen = ceil(maxHeight / rowHeight).toInt()
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + bottomSpace,
            ),
        ) {
            items(count = maxOf(shelvesWithBooks, shelvesToFillScreen)) { shelf ->
                val first = shelf * columns
                Shelf(columns = columns, bookWidth = bookWidth, itemCount = (itemCount - first).coerceAtLeast(0)) { i, width ->
                    slot(first + i, width)
                }
            }
        }
    }
}

/** 3 books per shelf; 2 when the system font is scaled up a lot, so titles stay readable. */
@Composable
private fun shelfColumns(): Int = if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) 2 else 3

@Composable
private fun PlaceholderCover(width: Dp) {
    Box(
        Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun EmptyLibrary(contentPadding: PaddingValues, onAddBook: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val bookWidth = shelfBookWidth(maxWidth, columns = 3)
        Column {
            Shelf(columns = 3, bookWidth = bookWidth, itemCount = 1) { _, width -> GhostBook(width) }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.library_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.library_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = stringResource(R.string.library_add_book),
                    onClick = onAddBook,
                    icon = R.drawable.ic_plus_circle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Dashed outline of a book: the empty-state placeholder from the design. */
@Composable
private fun GhostBook(width: Dp) {
    val outline = Color.White.copy(alpha = 0.45f)
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_book),
            contentDescription = null,
            tint = outline,
            modifier = Modifier.size(32.dp),
        )
    }
}

private const val PLACEHOLDER_COUNT = 6
private const val LARGE_FONT_SCALE = 1.3f

// Keeps the last shelf visible above the floating button.
private val FAB_SPACE = 88.dp

@Preview(showBackground = true)
@Composable
private fun LibraryWithBooksPreview() {
    ReaderTheme {
        LibraryContent(
            uiState = LibraryUiState(
                books = listOf(
                    Book(
                        id = 1,
                        title = "Cosmos",
                        author = "Carl Sagan",
                        currentPage = 120,
                        totalPages = 400,
                        status = BookStatus.READING,
                    ),
                    Book(id = 2, title = "Dune", author = "Frank Herbert"),
                ),
                isLoading = false,
            ),
            onBookClick = {},
            onAddBook = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    ReaderTheme {
        LibraryContent(
            uiState = LibraryUiState(isLoading = false),
            onBookClick = {},
            onAddBook = {},
        )
    }
}
