package io.github.fabiann1809.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.book.folder.WatchedFolder
import io.github.fabiann1809.reader.data.book.folder.WatchedFolderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** "Vigilar una carpeta" in the add-book sheet. Separate from LibraryViewModel on purpose: it only shares the screen. */
class WatchedFolderViewModel(
    private val manager: WatchedFolderManager,
    // Shown when the folder's own name can't be read (user-facing, so it comes from resources).
    private val fallbackName: String,
) : ViewModel() {

    val folder: StateFlow<WatchedFolder?> =
        manager.folder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initialValue = null)

    /** [folderUri] comes from the system folder picker. Its books are imported in the background. */
    fun watch(folderUri: String) {
        viewModelScope.launch { manager.watch(folderUri, fallbackName) }
    }

    fun stopWatching() {
        viewModelScope.launch { manager.stopWatching() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
