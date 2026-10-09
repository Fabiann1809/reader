package io.github.fabiann1809.reader.ui.newcollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.collection.COLOR_COUNT
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.collection.LibraryFilter
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewCollectionUiState(
    /** Every book of the library, newest first. */
    val books: List<Book> = emptyList(),
    val name: String = "",
    val colorIndex: Int = 0,
    /** Checked books in the order they were checked: the first ones show in the preview. */
    val selectedIds: List<Long> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val selectedBooks: List<Book>
        get() = selectedIds.mapNotNull { id -> books.find { it.id == id } }

    val canSave: Boolean
        get() = name.isNotBlank() && selectedIds.isNotEmpty() && !isSaving
}

/** The "Nueva colección" screen: a name, a color and the books that go in. */
class NewCollectionViewModel(
    bookRepository: BookRepository,
    private val collectionRepository: CollectionRepository,
    private val organizer: BookOrganizer,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val edits = MutableStateFlow(NewCollectionUiState())

    val uiState: StateFlow<NewCollectionUiState> = combine(bookRepository.observeBooks(), edits) { books, edits ->
        edits.copy(books = books.sortedWith(compareByDescending<Book> { it.createdAt }.thenByDescending { it.id }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), NewCollectionUiState())

    init {
        // Starts on the color the next collection would get anyway.
        viewModelScope.launch {
            val count = collectionRepository.observeCollections().first().size
            edits.update { it.copy(colorIndex = count % COLOR_COUNT) }
        }
    }

    fun onNameChange(name: String) = edits.update { it.copy(name = name) }

    fun onColorChange(colorIndex: Int) = edits.update { it.copy(colorIndex = colorIndex) }

    fun toggleBook(bookId: Long) = edits.update { state ->
        val ids = if (bookId in state.selectedIds) state.selectedIds - bookId else state.selectedIds + bookId
        state.copy(selectedIds = ids)
    }

    /** Creates the collection with its books and shows it in the library. */
    fun save() {
        val state = uiState.value
        if (!state.canSave) return
        edits.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = organizer.createCollectionWith(state.selectedIds, state.name, state.colorIndex)
            if (id != null) preferences.setLibraryFilter(LibraryFilter.Custom(id))
            edits.update { it.copy(isSaving = false, isSaved = id != null) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
