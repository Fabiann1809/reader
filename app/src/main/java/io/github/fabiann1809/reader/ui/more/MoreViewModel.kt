package io.github.fabiann1809.reader.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import io.github.fabiann1809.reader.data.note.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the "Más" tab shows besides its fixed entries: the AI key status and the notes summary. */
data class MoreUiState(
    val hasApiKey: Boolean = false,
    val noteCount: Int = 0,
    val bookCount: Int = 0,
)

class MoreViewModel(noteRepository: NoteRepository, apiKeyStore: ApiKeyStore) : ViewModel() {

    val uiState: StateFlow<MoreUiState> =
        combine(noteRepository.observeAllNotes(), apiKeyStore.hasApiKey) { notes, hasApiKey ->
            MoreUiState(
                hasApiKey = hasApiKey,
                noteCount = notes.size,
                bookCount = notes.map { it.bookId }.distinct().size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MoreUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
