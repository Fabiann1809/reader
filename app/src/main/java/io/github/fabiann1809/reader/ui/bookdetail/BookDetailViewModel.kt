package io.github.fabiann1809.reader.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.withStatus
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightRepository
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.data.session.ReadingSessionRepository
import io.github.fabiann1809.reader.util.endOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState

    data class Success(
        val book: Book,
        val notes: List<Note>,
        /** User collections, for "Añadir a colección". */
        val collections: List<Collection> = emptyList(),
        /** Ids of the user collections that already contain the book. */
        val collectionIds: Set<Long> = emptySet(),
        /** Highlights made in the reader, in reading order (T11.12). */
        val highlights: List<Highlight> = emptyList(),
        /** The book's review cards, newest first (T16.4's "Fichas"). */
        val flashcards: List<Flashcard> = emptyList(),
        /** How many of them are due today, for "Repasa este libro". */
        val dueCards: Int = 0,
        /** The book's reading sessions, newest first (T16.4's "Sesiones"). */
        val sessions: List<ReadingSession> = emptyList(),
    ) : BookDetailUiState {
        val remaining: RemainingReading? get() = remainingReading(book, sessions)

        /** What a quiz about this book is about: its notes and cards ("Ponme a prueba", T16.4). */
        val quizSource: String
            get() = (notes.map { it.content } + flashcards.map { "${it.front} — ${it.back}" })
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n")
    }

    // The book no longer exists (e.g. it was deleted).
    data object NotFound : BookDetailUiState
}

class BookDetailViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
    noteRepository: NoteRepository,
    collectionRepository: CollectionRepository,
    highlightRepository: HighlightRepository,
    private val organizer: BookOrganizer,
    private val sessionRepository: ReadingSessionRepository,
    flashcardRepository: FlashcardRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    // Cards and sessions, the T16.4 tabs, kept apart from the rest so each combine stays readable.
    private val study = combine(
        flashcardRepository.observeByBook(bookId),
        sessionRepository.observeByBook(bookId),
    ) { cards, sessions -> cards to sessions }

    // Notes are already sorted newest first by the query.
    val uiState: StateFlow<BookDetailUiState> =
        combine(
            combine(
                bookRepository.observeBook(bookId),
                noteRepository.observeNotes(bookId),
                collectionRepository.observeCollections(),
                collectionRepository.observeCollectionIdsOf(bookId),
                highlightRepository.observeHighlights(bookId),
            ) { book, notes, collections, collectionIds, highlights ->
                book?.let { BookDetailUiState.Success(it, notes, collections, collectionIds.toSet(), highlights) }
            },
            study,
        ) { detail, (cards, sessions) ->
            val endOfToday = endOfDay(now())
            detail?.copy(flashcards = cards, dueCards = cards.count { it.nextReviewAt <= endOfToday }, sessions = sessions)
                ?: BookDetailUiState.NotFound
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BookDetailUiState.Loading,
        )

    private val _isDeleted = MutableStateFlow(false)

    /** Becomes true once the book is deleted, so the screen can close itself. */
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    fun updateProgress(currentPage: Int, status: BookStatus) {
        viewModelScope.launch {
            // Read the stored book rather than the UI state so a stale screen can't overwrite newer data.
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.updateBook(book.withStatus(status, now()).copy(currentPage = currentPage))
            // A paper book is read outside the app: moving its page forward records what was read (T16.1).
            val pagesRead = currentPage - book.currentPage
            if (book.kind == BookKind.PHYSICAL && pagesRead > 0) {
                val at = now()
                sessionRepository.addSession(ReadingSession(bookId = bookId, startedAt = at, endedAt = at, pagesRead = pagesRead))
            }
        }
    }

    fun setFavorite(isFavorite: Boolean) {
        viewModelScope.launch { organizer.setFavorite(listOf(bookId), isFavorite) }
    }

    fun setInCollection(collectionId: Long, isIncluded: Boolean) {
        viewModelScope.launch { organizer.setInCollection(bookId, collectionId, isIncluded) }
    }

    /** Creates a collection that already holds this book. */
    fun createCollectionWithBook(name: String) {
        viewModelScope.launch { organizer.createCollectionWith(listOf(bookId), name) }
    }

    /** Deletes the book with its notes and files. */
    fun deleteBook() {
        viewModelScope.launch {
            organizer.delete(listOf(bookId))
            _isDeleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
