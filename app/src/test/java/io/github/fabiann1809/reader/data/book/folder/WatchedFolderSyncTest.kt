package io.github.fabiann1809.reader.data.book.folder

import io.github.fabiann1809.reader.data.book.importing.BookImporter
import io.github.fabiann1809.reader.data.book.importing.ImportResult
import io.github.fabiann1809.reader.testing.FakeFolderLister
import io.github.fabiann1809.reader.testing.FakeWatchedFolderStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchedFolderSyncTest {

    private val store = FakeWatchedFolderStore()
    private val lister = FakeFolderLister()

    // "lost…" files cannot be copied right now; "photo…" files are not books.
    private val imported = mutableListOf<String>()
    private var connectionBack = false
    private val importer = BookImporter { uri ->
        when {
            uri.contains("photo") -> ImportResult.Unsupported(null)
            uri.contains("lost") && !connectionBack -> ImportResult.Failed(null)
            else -> ImportResult.Imported(bookId = 1).also { imported += uri }
        }
    }
    private val sync = WatchedFolderSync(store, lister, importer)

    private suspend fun watch(vararg files: String) {
        store.setFolder(WatchedFolder("content://tree/Libros", "Libros"))
        lister.files["content://tree/Libros"] = files.toList()
    }

    @Test
    fun withoutAFolderNothingHappens() = runTest {
        assertEquals(FolderSyncResult.NotWatching, sync.sync())
    }

    @Test
    fun theBooksInTheFolderAreImportedOnce() = runTest {
        watch("a.epub", "b.pdf")

        assertEquals(FolderSyncResult.Done(imported = 2, failed = 0), sync.sync())
        assertEquals(FolderSyncResult.Done(imported = 0, failed = 0), sync.sync())
        assertEquals(listOf("a.epub", "b.pdf"), imported)
    }

    @Test
    fun aNewFileIsImportedOnTheNextCheck() = runTest {
        watch("a.epub")
        sync.sync()

        lister.files["content://tree/Libros"] = listOf("a.epub", "nuevo.epub")
        sync.sync()

        assertEquals(listOf("a.epub", "nuevo.epub"), imported)
    }

    @Test
    fun filesThatAreNotBooksAreNotRetriedButFailedCopiesAre() = runTest {
        watch("photo.pdf", "lost.epub")

        assertEquals(FolderSyncResult.Done(imported = 0, failed = 1), sync.sync())
        connectionBack = true
        assertEquals(FolderSyncResult.Done(imported = 1, failed = 0), sync.sync())
        assertEquals(listOf("lost.epub"), imported)
    }

    @Test
    fun anUnreadableFolderIsReported() = runTest {
        store.setFolder(WatchedFolder("content://tree/Borrada", "Borrada"))

        assertEquals(FolderSyncResult.FolderUnavailable, sync.sync())
    }
}
