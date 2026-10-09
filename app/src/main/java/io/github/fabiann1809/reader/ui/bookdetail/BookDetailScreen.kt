package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.NoteItem
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.StatusMessage
import io.github.fabiann1809.reader.ui.components.bookProgressText
import io.github.fabiann1809.reader.ui.components.labelRes
import io.github.fabiann1809.reader.ui.components.progressFraction
import io.github.fabiann1809.reader.ui.library.CollectionNameDialog
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.ui.voice.VoiceNoteControls
import io.github.fabiann1809.reader.ui.voice.rememberVoiceNoteControls
import io.github.fabiann1809.reader.util.formatDate
import kotlin.math.roundToInt

@Composable
fun BookDetailScreen(
    onNavigateUp: () -> Unit,
    onAddNote: () -> Unit,
    onAddFlashcard: () -> Unit = {},
    onNoteClick: (Long) -> Unit,
    // A note written on a passage opens the book there (T11.13).
    onOpenNoteInBook: (Note) -> Unit = {},
    onCapturePage: () -> Unit,
    onRead: () -> Unit = {},
    // "Con IA, si quieres" (T16.4): this book's due cards, and a quiz about its notes and cards.
    onReviewBook: () -> Unit = {},
    onQuizBook: (source: String, title: String) -> Unit = { _, _ -> },
    viewModel: BookDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeleted by viewModel.isDeleted.collectAsStateWithLifecycle()

    LaunchedEffect(isDeleted) {
        if (isDeleted) onNavigateUp()
    }

    BookDetailContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onUpdateProgress = viewModel::updateProgress,
        onDeleteBook = viewModel::deleteBook,
        onFavoriteChange = viewModel::setFavorite,
        onCollectionChange = viewModel::setInCollection,
        onCreateCollection = viewModel::createCollectionWithBook,
        onAddNote = onAddNote,
        onAddFlashcard = onAddFlashcard,
        onNoteClick = onNoteClick,
        onOpenNoteInBook = onOpenNoteInBook,
        onCapturePage = onCapturePage,
        onRead = onRead,
        voiceNotes = rememberVoiceNoteControls(),
        onReviewBook = onReviewBook,
        onQuizBook = onQuizBook,
    )
}

@Composable
fun BookDetailContent(
    uiState: BookDetailUiState,
    onNavigateUp: () -> Unit,
    onUpdateProgress: (currentPage: Int, status: BookStatus) -> Unit,
    onDeleteBook: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onCollectionChange: (collectionId: Long, isIncluded: Boolean) -> Unit,
    onCreateCollection: (String) -> Unit,
    onAddNote: () -> Unit,
    onAddFlashcard: () -> Unit = {},
    onNoteClick: (Long) -> Unit,
    // A note written on a passage opens the book there (T11.13).
    onOpenNoteInBook: (Note) -> Unit = {},
    onCapturePage: () -> Unit,
    modifier: Modifier = Modifier,
    onRead: () -> Unit = {},
    voiceNotes: VoiceNoteControls = VoiceNoteControls(),
    onReviewBook: () -> Unit = {},
    onQuizBook: (source: String, title: String) -> Unit = { _, _ -> },
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showProgressDialog by rememberSaveable { mutableStateOf(false) }
    var showCollectionSheet by rememberSaveable { mutableStateOf(false) }
    var showNewCollectionDialog by rememberSaveable { mutableStateOf(false) }
    // "Continuar leyendo": the cover swings open first and the reader follows (nothing to wait for with Reduce Motion).
    var isOpening by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(isOpening) {
        if (isOpening) {
            delay(Motion.BOOK_OPEN_MILLIS.toLong())
            onRead()
        }
    }
    val readBook = { if (reduceMotion) onRead() else isOpening = true }

    Scaffold(
        modifier = modifier,
        // The hero draws behind the status bar; the loading and missing states have their own bar.
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (uiState !is BookDetailUiState.Success) ReaderTopAppBar(title = "", onNavigateUp = onNavigateUp)
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            BookDetailUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            BookDetailUiState.NotFound -> Box(contentModifier, contentAlignment = Alignment.Center) {
                StatusMessage(
                    icon = R.drawable.ic_book,
                    title = stringResource(R.string.book_not_found),
                    message = stringResource(R.string.book_not_found_message),
                    action = {
                        OutlineButton(text = stringResource(R.string.book_back_to_library), onClick = onNavigateUp)
                    },
                )
            }
            is BookDetailUiState.Success -> BookDetailBody(
                state = uiState,
                onNavigateUp = onNavigateUp,
                onFavoriteChange = onFavoriteChange,
                onStatusChange = { status -> onUpdateProgress(uiState.book.currentPage, status) },
                onCollectionClick = { showCollectionSheet = true },
                onDeleteClick = { showDeleteDialog = true },
                onUpdateProgressClick = { showProgressDialog = true },
                onCapturePage = onCapturePage,
                onRead = readBook,
                isOpening = isOpening,
                onAddNote = onAddNote,
                onAddFlashcard = onAddFlashcard,
                onNoteClick = onNoteClick,
                onOpenNoteInBook = onOpenNoteInBook,
                voiceNotes = voiceNotes,
                onReviewBook = onReviewBook,
                onQuizBook = { onQuizBook(uiState.quizSource, uiState.book.title) },
                modifier = contentModifier,
            )
        }
    }

    if (uiState !is BookDetailUiState.Success) return

    if (showProgressDialog) {
        UpdateProgressDialog(
            book = uiState.book,
            onConfirm = { page, status ->
                onUpdateProgress(page, status)
                showProgressDialog = false
            },
            onDismiss = { showProgressDialog = false },
        )
    }

    if (showCollectionSheet) {
        AddToCollectionSheet(
            isFavorite = uiState.book.isFavorite,
            collections = uiState.collections,
            collectionIds = uiState.collectionIds,
            onFavoriteChange = onFavoriteChange,
            onCollectionChange = onCollectionChange,
            onNewCollection = { showNewCollectionDialog = true },
            onDismiss = { showCollectionSheet = false },
        )
    }

    // Shown over the sheet, so the new collection appears checked when the dialog closes.
    if (showNewCollectionDialog) {
        CollectionNameDialog(
            title = R.string.collection_new,
            confirm = R.string.collection_create,
            initialName = "",
            onConfirm = { name ->
                onCreateCollection(name)
                showNewCollectionDialog = false
            },
            onDismiss = { showNewCollectionDialog = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_book_title)) },
            text = { Text(stringResource(R.string.delete_book_message, uiState.book.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteBook()
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MoreMenu(onAddToCollectionClick: () -> Unit, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) { expanded = true },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dots_three_vertical),
                contentDescription = stringResource(R.string.more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.collection_add_title)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_books), contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToCollectionClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_book_title), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_trash), contentDescription = null, tint = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    expanded = false
                    onDeleteClick()
                },
            )
        }
    }
}

@Composable
private fun BookDetailBody(
    state: BookDetailUiState.Success,
    onNavigateUp: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onStatusChange: (BookStatus) -> Unit,
    onCollectionClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateProgressClick: () -> Unit,
    onCapturePage: () -> Unit,
    onRead: () -> Unit,
    isOpening: Boolean,
    onAddNote: () -> Unit,
    onAddFlashcard: () -> Unit = {},
    onNoteClick: (Long) -> Unit,
    // A note written on a passage opens the book there (T11.13).
    onOpenNoteInBook: (Note) -> Unit = {},
    voiceNotes: VoiceNoteControls = VoiceNoteControls(),
    onReviewBook: () -> Unit = {},
    onQuizBook: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(DetailTab.SUMMARY) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "hero") {
            DetailHero(
                book = state.book,
                onNavigateUp = onNavigateUp,
                onFavoriteChange = onFavoriteChange,
                isOpening = isOpening,
                menu = { MoreMenu(onAddToCollectionClick = onCollectionClick, onDeleteClick = onDeleteClick) },
            )
        }
        item(key = "progress") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProgressCard(state.book, state.remaining, onClick = onUpdateProgressClick)
                StatusSwitch(state.book.status, onStatusChange)
                DetailActionRow(state.book, onRead = onRead, onCapturePage = onCapturePage, onCollection = onCollectionClick)
            }
        }
        item(key = "tabs") {
            DetailTabRow(selected = tab, onSelect = { tab = it })
        }
        when (tab) {
            DetailTab.SUMMARY -> summaryTab(state, onReviewBook = onReviewBook, onQuizBook = onQuizBook)
            DetailTab.NOTES -> notesTab(state.notes, state.highlights, onAddNote, onNoteClick, onOpenNoteInBook, voiceNotes)
            DetailTab.CARDS -> cardsTab(state.flashcards, state.dueCards, onAddFlashcard, onReviewBook)
            DetailTab.SESSIONS -> sessionsTab(state.sessions)
        }
    }
}

/** "Notas y resaltados": the book's notes, then its highlights (T11.12). */
private fun LazyListScope.notesTab(
    notes: List<Note>,
    highlights: List<Highlight>,
    onAddNote: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onOpenNoteInBook: (Note) -> Unit,
    voiceNotes: VoiceNoteControls,
) {
    item(key = "notes_new") { AddLink(R.string.note_new_title, onAddNote) }
    if (notes.isEmpty()) {
        item(key = "notes_empty") {
            EmptyNotes()
        }
    } else {
        items(notes, key = { it.id }) { note ->
            NoteItem(
                note = note,
                onClick = { onNoteClick(note.id) },
                onOpenInBook = { onOpenNoteInBook(note) },
                isPlaying = voiceNotes.isPlaying(note),
                playbackFailed = voiceNotes.failed(note),
                onTogglePlayback = { voiceNotes.onToggle(note) },
            )
        }
    }
    highlightsSection(highlights)
}

@Composable
private fun EmptyNotes() {
    StatusMessage(
        icon = R.drawable.ic_note_pencil,
        title = stringResource(R.string.notes_empty),
        message = stringResource(R.string.notes_empty_message),
    )
}

private val COVER_WIDTH = 132.dp

@Preview(showBackground = true)
@Composable
private fun BookDetailPreview() {
    ReaderTheme {
        BookDetailContent(
            uiState = BookDetailUiState.Success(
                book = Book(
                    id = 1,
                    title = "Cosmos",
                    author = "Carl Sagan",
                    currentPage = 120,
                    totalPages = 400,
                    status = BookStatus.READING,
                ),
                notes = listOf(
                    Note(id = 1, bookId = 1, page = 12, content = "Somos polvo de estrellas."),
                    Note(
                        id = 2,
                        bookId = 1,
                        content = "Idea central: el universo es enorme y antiguo.",
                        type = NoteType.EXPLANATION,
                    ),
                ),
            ),
            onNavigateUp = {},
            onUpdateProgress = { _, _ -> },
            onDeleteBook = {},
            onFavoriteChange = {},
            onCollectionChange = { _, _ -> },
            onCreateCollection = {},
            onAddNote = {},
            onNoteClick = {},
            onCapturePage = {},
        )
    }
}
