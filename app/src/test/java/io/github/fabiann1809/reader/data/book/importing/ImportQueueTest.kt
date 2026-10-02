package io.github.fabiann1809.reader.data.book.importing

import io.github.fabiann1809.reader.testing.FakeFailedImportStore
import io.github.fabiann1809.reader.testing.FakeFileAccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportQueueTest {

    // Each file name decides the result: "bad…" is not a book, "lost…" cannot be copied, "dup…" is
    // already in the library.
    private val imported = mutableListOf<String>()
    private val gate = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val importer = BookImporter { uri ->
        gate[uri]?.await()
        when {
            uri.startsWith("bad") -> ImportResult.Unsupported(fileName = "$uri.rar")
            uri.startsWith("lost") -> ImportResult.Failed(fileName = null)
            uri.startsWith("dup") -> ImportResult.AlreadyInLibrary(title = "Libro de $uri")
            else -> ImportResult.Imported(bookId = 1).also { imported += uri }
        }
    }

    private val fileAccess = FakeFileAccess()
    private val store = FakeFailedImportStore()

    private fun TestScope.queue() = ImportQueue(importer, backgroundScope, fileAccess, store)

    @Test
    fun countsUpWhileImportingAndEndsIdleWhenAllWork() = runTest {
        val queue = queue()
        gate["b"] = CompletableDeferred()

        queue.add(listOf("a", "b", "c"))
        runCurrent()
        assertEquals(ImportStatus.Importing(done = 1, total = 3), queue.status.value)

        gate.getValue("b").complete(Unit)
        runCurrent()
        assertEquals(ImportStatus.Idle, queue.status.value)
        assertEquals(listOf("a", "b", "c"), imported)
    }

    @Test
    fun filesPickedDuringABatchJoinIt() = runTest {
        val queue = queue()
        gate["a"] = CompletableDeferred()

        queue.add(listOf("a", "b"))
        runCurrent()
        queue.add(listOf("c"))
        assertEquals(ImportStatus.Importing(done = 0, total = 3), queue.status.value)

        gate.getValue("a").complete(Unit)
        runCurrent()
        assertEquals(listOf("a", "b", "c"), imported)
    }

    @Test
    fun aBookAlreadyInTheLibraryIsReportedAndDismissed() = runTest {
        val queue = queue()

        queue.add(listOf("a", "dup1", "dup1"))
        runCurrent()

        assertEquals(ImportStatus.AlreadyInLibrary(titles = listOf("Libro de dup1")), queue.status.value)
        assertEquals(listOf("a"), imported)
        queue.dismiss()
        assertEquals(ImportStatus.Idle, queue.status.value)
    }

    @Test
    fun failuresWinOverRepeatedBooks() = runTest {
        val queue = queue()

        queue.add(listOf("dup1", "lost1"))
        runCurrent()

        assertEquals(
            ImportStatus.Failed(failures = listOf(FailedImport("lost1", null, isUnsupported = false)), importedCount = 0),
            queue.status.value,
        )
    }

    @Test
    fun failuresAreListedWithWhatDidImport() = runTest {
        val queue = queue()

        queue.add(listOf("a", "bad1", "lost1", "b"))
        runCurrent()

        assertEquals(
            ImportStatus.Failed(
                failures = listOf(
                    FailedImport("bad1", "bad1.rar", isUnsupported = true),
                    FailedImport("lost1", null, isUnsupported = false),
                ),
                importedCount = 2,
            ),
            queue.status.value,
        )
    }

    @Test
    fun retryImportsOnlyTheFailedFilesAsANewBatch() = runTest {
        val queue = queue()
        queue.add(listOf("a", "bad1"))
        runCurrent()

        queue.retry()
        runCurrent()

        assertEquals(listOf("a"), imported)
        assertEquals(ImportStatus.Failed(listOf(FailedImport("bad1", "bad1.rar", isUnsupported = true)), 0), queue.status.value)
    }

    @Test
    fun dismissForgetsTheFailures() = runTest {
        val queue = queue()
        queue.add(listOf("bad1"))
        runCurrent()

        queue.dismiss()

        assertEquals(ImportStatus.Idle, queue.status.value)
    }

    @Test
    fun pickingNothingChangesNothing() = runTest {
        val queue = queue()

        queue.add(emptyList())

        assertEquals(ImportStatus.Idle, queue.status.value)
    }

    @Test
    fun accessIsKeptOnlyForFilesThatFailed() = runTest {
        val queue = queue()

        queue.add(listOf("a", "bad1", "lost1"))
        runCurrent()

        assertEquals(setOf("bad1", "lost1"), fileAccess.kept)
        assertEquals(listOf("bad1", "lost1"), store.saved.map { it.uri })
    }

    @Test
    fun failuresFromAPreviousRunAreShownAgain() = runTest {
        store.saved = listOf(FailedImport("lost1", "roto.epub", isUnsupported = false))

        val queue = queue()
        runCurrent()

        assertEquals(ImportStatus.Failed(store.saved, importedCount = 0), queue.status.value)
    }

    @Test
    fun discardingForgetsTheFilesAndTheirAccess() = runTest {
        val queue = queue()
        queue.add(listOf("bad1"))
        runCurrent()

        queue.dismiss()
        runCurrent()

        assertEquals(emptySet<String>(), fileAccess.kept)
        assertEquals(emptyList<FailedImport>(), store.saved)
    }

    @Test
    fun aNewBatchLetsGoOfTheFilesItDoesNotRetry() = runTest {
        val queue = queue()
        queue.add(listOf("bad1", "lost1"))
        runCurrent()

        queue.add(listOf("a"))
        runCurrent()

        assertEquals(emptySet<String>(), fileAccess.kept)
        assertEquals(ImportStatus.Idle, queue.status.value)
    }
}
