package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    /**
     * The book is open in the [ReaderSession]; the navigator for its [format] takes it from there.
     * [chapter] and [progression] (0 to 1) are null until the navigator reports its first page.
     */
    data class Ready(
        val bookId: Long,
        val format: BookFormat,
        val title: String,
        val chapter: String? = null,
        val progression: Float? = null,
        val controlsVisible: Boolean = false,
    ) : ReaderUiState

    data class CannotOpen(val problem: OpenProblem) : ReaderUiState
}

class ReaderViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
    private val session: ReaderSession,
    // Injected so tests control time.
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId)
            _uiState.value = if (book == null) ReaderUiState.CannotOpen(OpenProblem.NO_FILE) else open(book)
        }
        viewModelScope.launch {
            session.locations.filter { it.bookId == bookId }.collect { location ->
                updateBook { it.copy(readingLocation = location.json) }
            }
        }
        viewModelScope.launch {
            session.position.filterNotNull().filter { it.bookId == bookId }.collect { position ->
                updateReady { it.copy(chapter = position.chapter, progression = position.progression?.toFloat()) }
            }
        }
    }

    /** A tap in the middle of the page shows the controls, or hides them. */
    fun toggleControls() = updateReady { it.copy(controlsVisible = !it.controlsVisible) }

    /** The first "Atrás" while reading shows the controls instead of leaving (design 03 §6). */
    fun showControls() = updateReady { it.copy(controlsVisible = true) }

    /** Goes to [progression] (0 to 1) of the book, chosen on the progress bar. */
    fun seekTo(progression: Float) {
        viewModelScope.launch { session.jumpTo(bookId, progression.toDouble()) }
    }

    private suspend fun open(book: Book): ReaderUiState {
        val problem = session.open(book)
        // The session only opens books with a readable format, so a null format here is a book without a file.
        val format = book.format
        if (problem != null || format == null) return ReaderUiState.CannotOpen(problem ?: OpenProblem.NO_FILE)
        markOpened()
        return ReaderUiState.Ready(bookId, format, book.title)
    }

    private fun updateReady(change: (ReaderUiState.Ready) -> ReaderUiState.Ready) =
        _uiState.update { state -> if (state is ReaderUiState.Ready) change(state) else state }

    // "Continuar leyendo" and the "Último leído" order use lastOpenedAt; opening a book also means reading it.
    private suspend fun markOpened() = updateBook { book ->
        book.copy(
            lastOpenedAt = now(),
            status = if (book.status == BookStatus.TO_READ) BookStatus.READING else book.status,
        )
    }

    // Reads the stored book first, so a change made elsewhere meanwhile is not overwritten.
    private suspend fun updateBook(change: (Book) -> Book) {
        val book = bookRepository.getBook(bookId) ?: return
        bookRepository.updateBook(change(book))
    }

    // Leaving the reader frees the book; a rotation keeps this ViewModel, so the book stays open.
    override fun onCleared() {
        session.close(bookId)
    }
}
