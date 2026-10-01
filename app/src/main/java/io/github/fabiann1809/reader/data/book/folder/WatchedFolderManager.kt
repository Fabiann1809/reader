package io.github.fabiann1809.reader.data.book.folder

import io.github.fabiann1809.reader.data.book.importing.FileAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Starting and stopping to watch a folder: its access, what is remembered about it and its checks. */
class WatchedFolderManager(
    private val store: WatchedFolderStore,
    private val lister: FolderLister,
    private val folderAccess: FileAccess,
    private val scheduler: FolderSyncScheduler,
) {
    val folder: Flow<WatchedFolder?> = store.folder

    /**
     * Watches [folderUri] (from the system folder picker) instead of any previous folder. The books
     * already in it are imported on the first check, as the user picked it to bring them in.
     */
    suspend fun watch(folderUri: String, fallbackName: String) {
        store.folder.first()?.let { previous -> if (previous.uri != folderUri) folderAccess.release(previous.uri) }
        // Checks run later in the background, so the picker's permission has to be kept.
        folderAccess.keep(folderUri)
        store.setFolder(WatchedFolder(folderUri, lister.name(folderUri) ?: fallbackName))
        scheduler.start()
    }

    suspend fun stopWatching() {
        val folder = store.folder.first() ?: return
        scheduler.stop()
        folderAccess.release(folder.uri)
        store.setFolder(null)
    }

    /** When the app opens, new books show up without waiting for the next periodic check. */
    suspend fun checkOnAppStart() {
        if (store.folder.first() != null) scheduler.checkNow()
    }
}
