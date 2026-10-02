package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.bookmark.BookmarkRepository
import io.github.fabiann1809.reader.data.prefs.ReadingPreferences
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.data.reader.TocEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val bookId: Long,
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val readingPreferences: ReadingPreferences,
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
            // Only once Ready, so the first list is not lost on the way.
            if (_uiState.value is ReaderUiState.Ready) followBookmarks()
        }
        viewModelScope.launch {
            // drop(1): the first settings were applied before the book opened.
            readingPreferences.settings.drop(1).collect { settings ->
                session.applyReadingSettings(settings)
                updateReady { it.copy(readingSettings = settings) }
            }
        }
        viewModelScope.launch {
            session.locations.filter { it.bookId == bookId }.collect { location ->
                updateBook { it.copy(readingLocation = location.json) }
            }
        }
        viewModelScope.launch {
            session.position.filterNotNull().filter { it.bookId == bookId }.collect { position ->
                updateReady {
                    it.copy(
                        chapter = position.chapter,
                        progression = position.progression?.toFloat(),
                        href = position.href,
                        position = position.position,
                    )
                }
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

    /** The top bar's bookmark: marks the open page, or unmarks it if it already was. */
    fun toggleBookmark() {
        val state = _uiState.value as? ReaderUiState.Ready ?: return
        viewModelScope.launch {
            val onPage = state.bookmarks.atPosition(state.position)
            if (onPage.isNotEmpty()) {
                onPage.forEach { bookmarkRepository.deleteBookmark(it) }
            } else {
                val location = session.currentLocation(bookId) ?: return@launch
                bookmarkRepository.addBookmark(
                    Bookmark(
                        bookId = bookId,
                        location = location,
                        position = state.position,
                        chapter = state.chapter,
                        progression = state.progression?.toDouble(),
                        createdAt = now(),
                    ),
                )
            }
        }
    }

    /** Opens the index and bookmarks sheet ("Índice" in the controls). */
    fun showContents() = updateReady { it.copy(contentsVisible = true) }

    fun hideContents() = updateReady { it.copy(contentsVisible = false) }

    /** Goes to a chapter of the index, back to the page with nothing on top. */
    fun goToChapter(entry: TocEntry) {
        leaveContentsForThePage()
        viewModelScope.launch { session.jumpToChapter(bookId, entry) }
    }

    /** Goes to a bookmark, back to the page with nothing on top. */
    fun goToBookmark(bookmark: Bookmark) {
        leaveContentsForThePage()
        session.jumpToLocation(bookId, bookmark.location)
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { bookmarkRepository.deleteBookmark(bookmark) }
    }

    private fun leaveContentsForThePage() = updateReady { it.copy(contentsVisible = false, controlsVisible = false) }

    private suspend fun followBookmarks() {
        bookmarkRepository.observeBookmarks(bookId).collect { list -> updateReady { it.copy(bookmarks = list) } }
    }

    /** The page's selected text, or null when it ends; the capsule replaces the controls meanwhile. */
    fun setSelection(selection: TextSelection?) = updateReady {
        it.copy(selection = selection, controlsVisible = if (selection != null) false else it.controlsVisible)
    }

    /** "Explicar" in the capsule: explains the selected text in a sheet over the page (T11.11). */
    fun explainSelection() = updateReady { state ->
        state.selection?.let { state.copy(explaining = it.text, selection = null) } ?: state
    }

    /** Closing the explainer leaves the reader on the same page. */
    fun closeExplanation() = updateReady { it.copy(explaining = null) }

    /** Opens the "Aa" sheet. */
    fun showTextSettings() = updateReady { it.copy(textSettingsVisible = true) }

    fun hideTextSettings() = updateReady { it.copy(textSettingsVisible = false) }

    /** Saves a change from the "Aa" sheet; the page shows it at once. A new size replaces a pinch's. */
    fun updateReadingSettings(change: (ReadingSettings) -> ReadingSettings) {
        val state = _uiState.value as? ReaderUiState.Ready ?: return
        val settings = change(state.readingSettings)
        if (settings.fontSize != state.readingSettings.fontSize) {
            session.adjustments = session.adjustments.copy(fontSize = null)
        }
        updateReady { it.copy(readingSettings = settings) }
        viewModelScope.launch { readingPreferences.setSettings(settings) }
    }

    private suspend fun open(book: Book): ReaderUiState {
        // Before the book opens, so its first page already has the reader's look.
        val settings = readingPreferences.settings.first()
        session.applyReadingSettings(settings)
        val problem = session.open(book)
        // The session only opens books with a readable format, so a null format here is a book without a file.
        val format = book.format
        if (problem != null || format == null) return ReaderUiState.CannotOpen(problem ?: OpenProblem.NO_FILE)
        markOpened()
        return ReaderUiState.Ready(
            bookId,
            format,
            book.title,
            tableOfContents = session.tableOfContents(bookId),
            positionCount = session.positionCount(bookId),
            readingSettings = settings,
        )
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
