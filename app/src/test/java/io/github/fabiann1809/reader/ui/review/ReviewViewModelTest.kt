package io.github.fabiann1809.reader.ui.review

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.testing.FakeBookRepository
import io.github.fabiann1809.reader.testing.FakeFlashcardRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.util.endOfDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = 1_790_900_000_000L
    private val books = FakeBookRepository(listOf(Book(id = 1, title = "Cosmos", author = ""), Book(id = 2, title = "Dune", author = "")))

    private fun card(id: Long, bookId: Long, nextReviewAt: Long, tag: NoteTag? = null) =
        Flashcard(id = id, bookId = bookId, front = "Ficha $id", back = "", tag = tag, nextReviewAt = nextReviewAt)

    private val cards = FakeFlashcardRepository(
        listOf(
            card(1, bookId = 2, nextReviewAt = now - 1_000, tag = NoteTag.TASK),
            card(2, bookId = 1, nextReviewAt = now - 5_000, tag = NoteTag.IDEA),
            // Later today: still for today.
            card(3, bookId = 1, nextReviewAt = endOfDay(now)),
            // Tomorrow: not yet.
            card(4, bookId = 1, nextReviewAt = endOfDay(now) + 1),
        ),
    )

    private fun viewModel() = ReviewViewModel(cards, books, now = { now })

    @Test
    fun todayShowsTheCardsDueAnyTimeTodayWithTheirBook() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(2L, 1L, 3L), state.dueCards.map { it.card.id })
        assertEquals("Cosmos", state.dueCards.first().bookTitle)
        assertEquals(1, state.groups.size)
    }

    @Test
    fun byBookGroupsThemUnderEachTitle() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.selectMode(ReviewMode.BY_BOOK)

        val groups = viewModel.uiState.value.groups
        assertEquals(listOf("Cosmos", "Dune"), groups.map { it.title })
        assertEquals(listOf(2L, 3L), groups.first().cards.map { it.card.id })
    }

    @Test
    fun byTagFollowsTheLabelsOrderWithUntaggedLast() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.launchIn(backgroundScope)

        viewModel.selectMode(ReviewMode.BY_TAG)

        assertEquals(listOf(NoteTag.IDEA, NoteTag.TASK, null), viewModel.uiState.value.groups.map { it.tag })
    }

    @Test
    fun theTabIsDottedOnlyWhileThereAreCardsForToday() = runTest(mainDispatcherRule.testDispatcher) {
        assertTrue(PendingReviewsViewModel(cards, now = { now }).hasPending.let { flow -> flow.first { it } })

        val tomorrowOnly = FakeFlashcardRepository(listOf(card(4, bookId = 1, nextReviewAt = endOfDay(now) + 1)))
        val pending = PendingReviewsViewModel(tomorrowOnly, now = { now })
        pending.hasPending.launchIn(backgroundScope)
        assertFalse(pending.hasPending.value)
    }
}
