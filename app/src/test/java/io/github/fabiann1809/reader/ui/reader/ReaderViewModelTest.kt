package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.BookStatus
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.prefs.ReadingFont
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.prefs.ReadingTheme
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.data.reader.ReadingAdjustments
import io.github.fabiann1809.reader.data.reader.ReadingLocation
import io.github.fabiann1809.reader.data.reader.ReadingPosition
import io.github.fabiann1809.reader.data.reader.TocEntry
import io.github.fabiann1809.reader.testing.FakeBookmarkRepository
import io.github.fabiann1809.reader.testing.FakeReadingPreferences
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val books = FakeBookRepository(
        listOf(
            Book(id = 1, title = "El principito", author = "Saint-Exupéry", format = BookFormat.EPUB),
            Book(id = 2, title = "Dune", author = "Frank Herbert", status = BookStatus.FINISHED, format = BookFormat.EPUB),
            Book(id = 3, title = "Cosmos", author = "Carl Sagan", format = BookFormat.PDF),
        ),
    )

    /** Opens every book unless told what problem to report; [locations] and [position] are driven by the test. */
    private class FakeSession(private val problem: OpenProblem? = null) : ReaderSession {
        val opened = mutableListOf<Long>()
        val jumpedTo = mutableListOf<Double>()
        val jumpedToChapters = mutableListOf<TocEntry>()
        val jumpedToLocations = mutableListOf<String>()
        var location: String? = "{\"href\":\"c1.xhtml\"}"
        val toc = listOf(TocEntry(index = 0, title = "Capítulo 1", level = 0, href = "c1.xhtml"))
        override val locations = MutableSharedFlow<ReadingLocation>(extraBufferCapacity = 8)
        override val position = MutableStateFlow<ReadingPosition?>(null)
        override val jumps = MutableSharedFlow<Locator>()
        override var adjustments = ReadingAdjustments()
        override val readingSettings = MutableStateFlow(ReadingSettings())

        override fun applyReadingSettings(settings: ReadingSettings) {
            readingSettings.value = settings
        }

        override suspend fun open(book: Book): OpenProblem? = problem.also { if (it == null) opened += book.id }

        override fun publication(bookId: Long): Publication? = null

        override fun initialLocator(bookId: Long): Locator? = null

        override fun reportLocation(bookId: Long, locator: Locator) = Unit

        override suspend fun jumpTo(bookId: Long, totalProgression: Double) {
            jumpedTo += totalProgression
        }

        override fun tableOfContents(bookId: Long): List<TocEntry> = toc

        override fun positionCount(bookId: Long): Int? = 120

        override suspend fun jumpToChapter(bookId: Long, entry: TocEntry) {
            jumpedToChapters += entry
        }

        override fun jumpToLocation(bookId: Long, location: String) {
            jumpedToLocations += location
        }

        override fun currentLocation(bookId: Long): String? = location

        override fun close(bookId: Long) = Unit
    }

    private val bookmarks = FakeBookmarkRepository()
    private val readingPreferences = FakeReadingPreferences(ReadingSettings(theme = ReadingTheme.SEPIA))

    private fun viewModel(bookId: Long, session: ReaderSession) = ReaderViewModel(
        bookId = bookId,
        bookRepository = books,
        bookmarkRepository = bookmarks,
        readingPreferences = readingPreferences,
        session = session,
        now = { 5_000L },
    )

    @Test
    fun anOpenedBookIsReady() {
        val session = FakeSession()

        val viewModel = viewModel(bookId = 1, session = session)

        assertEquals(
            ReaderUiState.Ready(
                bookId = 1,
                format = BookFormat.EPUB,
                title = "El principito",
                tableOfContents = session.toc,
                positionCount = 120,
                readingSettings = ReadingSettings(theme = ReadingTheme.SEPIA),
            ),
            viewModel.uiState.value,
        )
        assertEquals(listOf(1L), session.opened)
    }

    @Test
    fun aPdfIsReadyWithItsFormat() {
        val viewModel = viewModel(bookId = 3, session = FakeSession())

        // ReaderScreen picks the PDF navigator from it.
        assertEquals(BookFormat.PDF, (viewModel.uiState.value as ReaderUiState.Ready).format)
    }

    @Test
    fun aCenterTapShowsTheControlsAndAnotherHidesThem() {
        val viewModel = viewModel(bookId = 1, session = FakeSession())

        viewModel.toggleControls()
        assertTrue(ready(viewModel).controlsVisible)

        viewModel.toggleControls()
        assertFalse(ready(viewModel).controlsVisible)
    }

    @Test
    fun backShowsTheControlsWithoutHidingThem() {
        val viewModel = viewModel(bookId = 1, session = FakeSession())

        viewModel.showControls()
        viewModel.showControls()

        assertTrue(ready(viewModel).controlsVisible)
    }

    @Test
    fun theControlsFollowThePageOfThisBook() {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)

        session.position.value = ReadingPosition(bookId = 1, chapter = "Capítulo 4", progression = 0.25)
        assertEquals("Capítulo 4", ready(viewModel).chapter)
        assertEquals(0.25f, ready(viewModel).progression!!, 0.0001f)

        session.position.value = ReadingPosition(bookId = 2, chapter = "Otro libro", progression = 0.9)
        assertEquals("Capítulo 4", ready(viewModel).chapter)
    }

    @Test
    fun seekingAsksTheSessionToJump() {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)

        viewModel.seekTo(0.5f)

        assertEquals(listOf(0.5), session.jumpedTo)
    }

    @Test
    fun theBookmarkButtonMarksThePageAndThenUnmarksIt() = runTest {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)
        session.position.value = ReadingPosition(bookId = 1, chapter = "Capítulo 1", progression = 0.2, href = "c1.xhtml", position = 4)

        viewModel.toggleBookmark()

        val bookmark = bookmarks.currentBookmarks.single()
        assertEquals(session.location, bookmark.location)
        assertEquals(4, bookmark.position)
        assertEquals("Capítulo 1", bookmark.chapter)
        assertEquals(5_000L, bookmark.createdAt)
        assertTrue(ready(viewModel).pageIsBookmarked)

        viewModel.toggleBookmark()

        assertTrue(bookmarks.currentBookmarks.isEmpty())
        assertFalse(ready(viewModel).pageIsBookmarked)
    }

    @Test
    fun nothingIsMarkedBeforeTheFirstPage() = runTest {
        val session = FakeSession().apply { location = null }
        val viewModel = viewModel(bookId = 1, session = session)

        viewModel.toggleBookmark()

        assertTrue(bookmarks.currentBookmarks.isEmpty())
    }

    @Test
    fun choosingAChapterGoesThereAndLeavesThePageClear() {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)
        viewModel.toggleControls()
        viewModel.showContents()
        assertTrue(ready(viewModel).contentsVisible)

        viewModel.goToChapter(session.toc.single())

        assertEquals(session.toc, session.jumpedToChapters)
        assertFalse(ready(viewModel).contentsVisible)
        assertFalse(ready(viewModel).controlsVisible)
    }

    @Test
    fun choosingABookmarkGoesToItsLocation() = runTest {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)
        bookmarks.addBookmark(Bookmark(bookId = 1, location = "{\"saved\":true}", position = 9))
        viewModel.showContents()

        viewModel.goToBookmark(ready(viewModel).bookmarks.single())

        assertEquals(listOf("{\"saved\":true}"), session.jumpedToLocations)
        assertFalse(ready(viewModel).contentsVisible)
    }

    @Test
    fun deletingABookmarkRemovesItFromTheList() = runTest {
        val viewModel = viewModel(bookId = 1, session = FakeSession())
        bookmarks.addBookmark(Bookmark(bookId = 1, location = "{}", position = 9))

        viewModel.deleteBookmark(ready(viewModel).bookmarks.single())

        assertTrue(ready(viewModel).bookmarks.isEmpty())
    }

    @Test
    fun theSavedSettingsReachTheNavigatorBeforeTheBookOpens() {
        val session = FakeSession()

        viewModel(bookId = 1, session = session)

        assertEquals(ReadingTheme.SEPIA, session.readingSettings.value.theme)
    }

    @Test
    fun aChangeInTheSheetIsSavedShownAndSentToTheNavigator() = runTest {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)
        viewModel.showTextSettings()
        assertTrue(ready(viewModel).textSettingsVisible)

        viewModel.updateReadingSettings { it.copy(theme = ReadingTheme.NIGHT, font = ReadingFont.LORA) }

        assertEquals(ReadingTheme.NIGHT, readingPreferences.settings.value.theme)
        assertEquals(ReadingFont.LORA, ready(viewModel).readingSettings.font)
        assertEquals(ReadingTheme.NIGHT, session.readingSettings.value.theme)
        viewModel.hideTextSettings()
        assertFalse(ready(viewModel).textSettingsVisible)
    }

    @Test
    fun aNewSizeInTheSheetReplacesAPinch() {
        val session = FakeSession()
        val viewModel = viewModel(bookId = 1, session = session)
        session.adjustments = ReadingAdjustments(fontSize = 2.5, brightness = 0.4f)

        viewModel.updateReadingSettings { it.copy(lineHeight = 2.0) }
        assertEquals(2.5, session.adjustments.fontSize!!, 0.0001)

        viewModel.updateReadingSettings { it.copy(fontSize = 1.3) }
        assertNull(session.adjustments.fontSize)
        // The brightness is not a text setting: it stays.
        assertEquals(0.4f, session.adjustments.brightness!!, 0.0001f)
    }

    @Test
    fun aSelectionHidesTheControlsAndEndingItKeepsThemHidden() {
        val viewModel = viewModel(bookId = 1, session = FakeSession())
        viewModel.toggleControls()
        val selection = TextSelection("memoria", SelectionBounds(10f, 20f, 110f, 60f))

        viewModel.setSelection(selection)
        assertEquals(selection, ready(viewModel).selection)
        assertFalse(ready(viewModel).controlsVisible)

        viewModel.setSelection(null)
        assertNull(ready(viewModel).selection)
        assertFalse(ready(viewModel).controlsVisible)
    }

    private fun ready(viewModel: ReaderViewModel) = viewModel.uiState.value as ReaderUiState.Ready

    @Test
    fun aBookThatCannotOpenSaysWhy() {
        val viewModel = viewModel(bookId = 1, session = FakeSession(OpenProblem.NOT_SUPPORTED_YET))

        assertEquals(ReaderUiState.CannotOpen(OpenProblem.NOT_SUPPORTED_YET), viewModel.uiState.value)
    }

    @Test
    fun aDeletedBookHasNoFile() {
        val viewModel = viewModel(bookId = 99, session = FakeSession())

        assertEquals(ReaderUiState.CannotOpen(OpenProblem.NO_FILE), viewModel.uiState.value)
    }

    @Test
    fun openingRecordsWhenAndStartsReading() = runTest {
        viewModel(bookId = 1, session = FakeSession())
        viewModel(bookId = 2, session = FakeSession())

        assertEquals(5_000L, books.getBook(1)?.lastOpenedAt)
        assertEquals(BookStatus.READING, books.getBook(1)?.status)
        // A finished book being reread stays finished.
        assertEquals(BookStatus.FINISHED, books.getBook(2)?.status)
    }

    @Test
    fun aBookThatCannotOpenIsNotMarkedOpened() = runTest {
        viewModel(bookId = 1, session = FakeSession(OpenProblem.UNREADABLE))

        assertNull(books.getBook(1)?.lastOpenedAt)
    }

    @Test
    fun pageTurnsOfThisBookAreSaved() = runTest {
        val session = FakeSession()
        viewModel(bookId = 1, session = session)

        session.locations.emit(ReadingLocation(bookId = 2, json = "{\"other\":true}"))
        session.locations.emit(ReadingLocation(bookId = 1, json = "{\"page\":3}"))

        assertEquals("{\"page\":3}", books.getBook(1)?.readingLocation)
        assertNull(books.getBook(2)?.readingLocation)
    }
}
