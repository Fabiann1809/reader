package io.github.fabiann1809.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightRepository
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The open book's notes (newest first) and highlights (in reading order), for the reader's "Notas" sheet. */
data class ReaderNotesUiState(
    val notes: List<Note> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val isLoading: Boolean = true,
)

class ReaderNotesViewModel(
    bookId: Long,
    noteRepository: NoteRepository,
    highlightRepository: HighlightRepository,
) : ViewModel() {

    val uiState: StateFlow<ReaderNotesUiState> =
        combine(noteRepository.observeNotes(bookId), highlightRepository.observeHighlights(bookId)) { notes, highlights ->
            ReaderNotesUiState(notes, highlights, isLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReaderNotesUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
