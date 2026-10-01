package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.book.folder.FolderLister
import io.github.fabiann1809.reader.data.book.folder.FolderSyncScheduler
import io.github.fabiann1809.reader.data.book.folder.WatchedFolder
import io.github.fabiann1809.reader.data.book.folder.WatchedFolderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWatchedFolderStore : WatchedFolderStore {
    private val current = MutableStateFlow<WatchedFolder?>(null)
    val handled = mutableSetOf<String>()

    override val folder: Flow<WatchedFolder?> = current

    override suspend fun setFolder(folder: WatchedFolder?) {
        handled.clear()
        current.value = folder
    }

    override suspend fun handledFiles(): Set<String> = handled.toSet()

    override suspend fun markHandled(fileUris: Collection<String>) {
        handled += fileUris
    }
}

/** Folders and their files; a folder missing from [files] can't be read. */
class FakeFolderLister(val files: MutableMap<String, List<String>> = mutableMapOf()) : FolderLister {
    override suspend fun bookFiles(folderUri: String): List<String>? = files[folderUri]

    override suspend fun name(folderUri: String): String? = folderUri.substringAfterLast('/').ifEmpty { null }
}

class FakeFolderSyncScheduler : FolderSyncScheduler {
    var isRunning = false
    var extraChecks = 0

    override fun start() {
        isRunning = true
    }

    override fun checkNow() {
        extraChecks++
    }

    override fun stop() {
        isRunning = false
    }
}
