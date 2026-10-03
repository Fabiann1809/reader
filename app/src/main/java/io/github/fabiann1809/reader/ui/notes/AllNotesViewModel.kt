package io.github.fabiann1809.reader.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.util.matchesSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A note together with the title of the book it belongs to. */
data class NoteWithBook(val note: Note, val bookTitle: String)

/**
 * [notes] are the ones that match [query] and [type] (T17.4: search and filter by kind);
 * [hasNotes] tells "no notes yet" from "nothing matches".
 */
data class AllNotesUiState(
    val notes: List<NoteWithBook> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    // Null shows every kind.
    val type: NoteType? = null,
    val hasNotes: Boolean = false,
)

class AllNotesViewModel(bookRepository: BookRepository, noteRepository: NoteRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val type = MutableStateFlow<NoteType?>(null)

    // Notes come newest first from the repository; the books are only used to show each note's title.
    val uiState: StateFlow<AllNotesUiState> =
        combine(noteRepository.observeAllNotes(), bookRepository.observeBooks(), query, type) { notes, books, query, type ->
            val titles = books.associate { it.id to it.title }
            val all = notes.mapNotNull { note -> titles[note.bookId]?.let { NoteWithBook(note, it) } }
            AllNotesUiState(
                notes = all.filter { (type == null || it.note.type == type) && it.matches(query) },
                isLoading = false,
                query = query,
                type = type,
                hasNotes = all.isNotEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AllNotesUiState(),
        )

    fun search(text: String) {
        query.value = text
    }

    /** A chip: one kind of note, or every kind again (null). */
    fun showType(noteType: NoteType?) {
        type.value = noteType
    }

    // The note, the passage it is about and its book's title.
    private fun NoteWithBook.matches(query: String) = matchesSearch(query, note.content, note.sourceText, bookTitle)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
