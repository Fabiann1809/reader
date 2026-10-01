package io.github.fabiann1809.reader.data.book.folder

import io.github.fabiann1809.reader.data.book.importing.BookImporter
import io.github.fabiann1809.reader.data.book.importing.ImportResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What one check of the watched folder did. */
sealed interface FolderSyncResult {
    /** No folder is being watched. */
    data object NotWatching : FolderSyncResult

    /** The folder is gone or Reader lost access to it (e.g. it was deleted or the permission revoked). */
    data object FolderUnavailable : FolderSyncResult

    /** [imported] new books were added; [failed] files could not be copied now and will be tried again. */
    data class Done(val imported: Int, val failed: Int) : FolderSyncResult
}

/**
 * Imports the files that appeared in the watched folder since the last check. Each file is handled
 * once: once imported, or once found not to be a book, it is never looked at again. Files that could
 * not be copied (e.g. a cloud file without connection) are tried again next time.
 */
class WatchedFolderSync(
    private val store: WatchedFolderStore,
    private val lister: FolderLister,
    private val importer: BookImporter,
) {
    // The periodic check and the one when the app opens can overlap; one at a time keeps a new file
    // from being imported twice.
    private val mutex = Mutex()

    suspend fun sync(): FolderSyncResult = mutex.withLock { syncNow() }

    private suspend fun syncNow(): FolderSyncResult {
        val folder = store.folder.first() ?: return FolderSyncResult.NotWatching
        val files = lister.bookFiles(folder.uri) ?: return FolderSyncResult.FolderUnavailable
        val handled = store.handledFiles()
        var imported = 0
        var failed = 0
        for (uri in files.filterNot { it in handled }) {
            when (importer.import(uri)) {
                is ImportResult.Imported -> imported++
                is ImportResult.Unsupported -> Unit
                is ImportResult.Failed -> {
                    failed++
                    continue
                }
            }
            // Saved file by file, so an interrupted check repeats at most the file it was on.
            store.markHandled(listOf(uri))
        }
        return FolderSyncResult.Done(imported, failed)
    }
}
