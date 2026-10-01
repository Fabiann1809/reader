package io.github.fabiann1809.reader.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A note together with the title of the book it belongs to. */
data class NoteWithBook(val note: Note, val bookTitle: String)

data class AllNotesUiState(
    val notes: List<NoteWithBook> = emptyList(),
    val isLoading: Boolean = true,
)

class AllNotesViewModel(bookRepository: BookRepository, noteRepository: NoteRepository) : ViewModel() {

    // Notes come newest first from the repository; the books are only used to show each note's title.
    val uiState: StateFlow<AllNotesUiState> =
        combine(noteRepository.observeAllNotes(), bookRepository.observeBooks()) { notes, books ->
            val titles = books.associate { it.id to it.title }
            AllNotesUiState(
                notes = notes.mapNotNull { note -> titles[note.bookId]?.let { NoteWithBook(note, it) } },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AllNotesUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
