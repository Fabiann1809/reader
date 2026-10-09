package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.MAX_QUIZ_TEXT_LENGTH
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.bookmark.BookmarkRepository
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightColor
import io.github.fabiann1809.reader.data.highlight.HighlightRepository
import io.github.fabiann1809.reader.data.prefs.ChapterSuggestions
import io.github.fabiann1809.reader.data.prefs.ReadingPreferences
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.data.reader.SearchHit
import io.github.fabiann1809.reader.data.reader.TocEntry
import io.github.fabiann1809.reader.data.session.ReadingSessionTracker
import io.github.fabiann1809.reader.ocr.TextRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val highlightRepository: HighlightRepository,
    private val readingPreferences: ReadingPreferences,
    private val textRecognizer: TextRecognizer,
    private val session: ReaderSession,
    private val chapterSuggestions: ChapterSuggestions,
    private val sessionTracker: ReadingSessionTracker,
    // A place to open the book at (e.g. a note's), instead of where the reader left off.
    private val startAt: String? = null,
    // Injected so tests control time.
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId)
            _uiState.value = if (book == null) ReaderUiState.CannotOpen(OpenProblem.NO_FILE) else open(book)
            // Only once Ready, so the first lists are not lost on the way.
            if (_uiState.value is ReaderUiState.Ready) {
                launch { followBookmarks() }
                launch { followHighlights() }
            }
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
                // A digital book's page is Readium's position, out of the book's positions (T16.6):
                // the detail, the shelf and the cover then show its real progress instead of "Página 0".
                val position = session.position.value?.takeIf { it.bookId == bookId }?.position
                val total = session.positionCount(bookId)
                updateBook { book ->
                    book.copy(
                        readingLocation = location.json,
                        currentPage = position ?: book.currentPage,
                        totalPages = total ?: book.totalPages,
                    )
                }
            }
        }
        viewModelScope.launch {
            session.position.filterNotNull().filter { it.bookId == bookId }.collect { position ->
                (_uiState.value as? ReaderUiState.Ready)?.let { suggestIfChapterEnded(it, position.href) }
                sessionTracker.moved(position.progression, position.chapterProgression)
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

    private suspend fun followHighlights() {
        highlightRepository.observeHighlights(bookId).collect(session::showHighlights)
    }

    private suspend fun followBookmarks() {
        bookmarkRepository.observeBookmarks(bookId).collect { list -> updateReady { it.copy(bookmarks = list) } }
    }

    /** The page's selected text, or null when it ends; the capsule replaces the controls meanwhile. */
    fun setSelection(selection: TextSelection?) = updateReady {
        it.copy(selection = selection, controlsVisible = if (selection != null) false else it.controlsVisible)
    }

    /** "Resaltar" in the capsule: saves the selection highlighted in [color]; the page draws it at once. */
    fun highlightSelection(color: HighlightColor) {
        val state = _uiState.value as? ReaderUiState.Ready ?: return
        val selection = state.selection?.takeIf { it.location.isNotBlank() } ?: return
        updateReady { it.copy(selection = null) }
        viewModelScope.launch {
            highlightRepository.addHighlight(
                Highlight(
                    bookId = bookId,
                    location = selection.location,
                    text = selection.text,
                    color = color,
                    progression = state.progression?.toDouble(),
                    createdAt = now(),
                ),
            )
        }
    }

    /** "Explicar" in the capsule: explains the selected text in a sheet over the page (T11.11). */
    fun explainSelection() = updateReady { state ->
        state.selection?.let { state.copy(explaining = it.text, selection = null) } ?: state
    }

    /** "IA" in a PDF: mark a zone of the page to explain it (T11.14), with nothing else on top. */
    fun startZonePicking() = updateReady { it.copy(zonePicking = ZonePicking.MARKING, controlsVisible = false) }

    fun cancelZonePicking() = updateReady { it.copy(zonePicking = null) }

    /** Reads the marked zone's picture ([imageUri]) with the OCR and explains its text; null means it couldn't be saved. */
    fun explainZone(imageUri: String?) {
        if (imageUri == null) {
            updateReady { it.copy(zonePicking = ZonePicking.NO_TEXT) }
            return
        }
        updateReady { it.copy(zonePicking = ZonePicking.READING) }
        viewModelScope.launch {
            val text = textRecognizer.recognize(imageUri).getOrNull()?.text?.takeIf { it.isNotBlank() }
            updateReady {
                if (text == null) it.copy(zonePicking = ZonePicking.NO_TEXT) else it.copy(zonePicking = null, explaining = text)
            }
        }
    }

    /** Closing the explainer leaves the reader on the same page. */
    fun closeExplanation() = updateReady { it.copy(explaining = null) }

    /** Opens the "Aa" sheet. */
    fun showTextSettings() = updateReady { it.copy(textSettingsVisible = true) }

    fun hideTextSettings() = updateReady { it.copy(textSettingsVisible = false) }

    fun startVoiceNote() = updateReady { it.copy(recordingVoice = true) }

    fun closeVoiceNote() = updateReady { it.copy(recordingVoice = false) }

    /** The page shown now as a Locator in JSON, to anchor a voice note there; null before the first page. */
    fun currentLocation(): String? = session.currentLocation(bookId)

    fun showMenu() = updateReady { it.copy(menuVisible = true, chapterUnreadable = false) }

    fun hideMenu() = updateReady { it.copy(menuVisible = false) }

    fun openSearch() = updateReady { it.copy(search = ReaderSearchState(), controlsVisible = false) }

    fun closeSearch() {
        searchJob?.cancel()
        updateReady { it.copy(search = null) }
    }

    /** Searches the book for [query] once the typing pauses; a blank one clears the results. */
    fun onSearchQuery(query: String) {
        searchJob?.cancel()
        updateReady { it.copy(search = ReaderSearchState(query = query, isSearching = query.isNotBlank())) }
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(SEARCH_PAUSE_MILLIS)
            val hits = session.search(bookId, query.trim())
            updateReady { state ->
                state.copy(search = state.search?.copy(results = hits.orEmpty(), isSearching = false, unsupported = hits == null))
            }
        }
    }

    /** Goes to a match, back to the page with nothing on top. */
    fun goToSearchResult(hit: SearchHit) {
        closeSearch()
        session.jumpToLocation(bookId, hit.location)
    }

    fun showNotes() = updateReady { it.copy(notesVisible = true) }

    fun hideNotes() = updateReady { it.copy(notesVisible = false) }

    /** Goes to a note or highlight of the "Notas" sheet, back to the page with nothing on top. */
    fun goToLocation(location: String) {
        updateReady { it.copy(notesVisible = false, controlsVisible = false) }
        session.jumpToLocation(bookId, location)
    }

    /** "Ponme a prueba" in the menu: reads the open chapter and asks to open a quiz of [count] questions about it. */
    fun startChapterQuiz(count: Int) {
        val state = _uiState.value as? ReaderUiState.Ready ?: return
        val href = state.href ?: return
        viewModelScope.launch {
            // A very long chapter is cut: its beginning is still enough for a few questions.
            val text = session.chapterText(bookId, href)?.take(MAX_QUIZ_TEXT_LENGTH)
            updateReady {
                if (text == null) {
                    it.copy(chapterUnreadable = true)
                } else {
                    it.copy(menuVisible = false, chapterQuiz = ChapterQuiz(text, count, state.chapter ?: state.title))
                }
            }
        }
    }

    /** The reader opened the quiz. */
    fun chapterQuizStarted() = updateReady { it.copy(chapterQuiz = null) }

    /** "Ponme a prueba" in the end-of-chapter suggestion: a quiz about the chapter just finished. */
    fun quizFinishedChapter() {
        val end = (_uiState.value as? ReaderUiState.Ready)?.chapterEnd ?: return
        dismissChapterEnd()
        viewModelScope.launch {
            val text = session.chapterText(bookId, end.href)?.take(MAX_QUIZ_TEXT_LENGTH) ?: return@launch
            updateReady { it.copy(chapterQuiz = ChapterQuiz(text, CHAPTER_END_QUIZ_SIZE, end.title)) }
        }
    }

    fun dismissChapterEnd() = updateReady { it.copy(chapterEnd = null) }

    // Once per chapter (T15.5), and only in EPUBs, whose chapters have text for the quiz.
    private fun suggestIfChapterEnded(state: ReaderUiState.Ready, newHref: String?) {
        if (state.format != BookFormat.EPUB) return
        val end = state.tableOfContents.finishedChapter(state.href, newHref) ?: return
        viewModelScope.launch {
            if (!chapterSuggestions.isEnabled() || chapterSuggestions.wasSuggested(bookId, end.href)) return@launch
            chapterSuggestions.markSuggested(bookId, end.href)
            updateReady { it.copy(chapterEnd = end) }
        }
    }

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
        val problem = session.open(book, startAt)
        // The session only opens books with a readable format, so a null format here is a book without a file.
        val format = book.format
        if (problem != null || format == null) return ReaderUiState.CannotOpen(problem ?: OpenProblem.NO_FILE)
        markOpened()
        sessionTracker.start(bookId)
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
    /** The app went to the background (or the screen off): the reading session ends there (T16.1). */
    fun pauseReading() = sessionTracker.stop()

    /** Back in the reader: a new session starts. */
    fun resumeReading() {
        if (_uiState.value is ReaderUiState.Ready) sessionTracker.start(bookId)
    }

    override fun onCleared() {
        sessionTracker.stop()
        session.close(bookId)
    }
}

// The end-of-chapter suggestion's quiz: short, it interrupts reading.
private const val CHAPTER_END_QUIZ_SIZE = 3

// How long typing must pause before the book is searched.
private const val SEARCH_PAUSE_MILLIS = 300L
