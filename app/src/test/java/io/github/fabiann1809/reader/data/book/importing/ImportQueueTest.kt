package io.github.fabiann1809.reader.data.book.importing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportQueueTest {

    // Each file name decides the result: "bad…" is not a book, "lost…" cannot be copied.
    private val imported = mutableListOf<String>()
    private val gate = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val importer = BookImporter { uri ->
        gate[uri]?.await()
        when {
            uri.startsWith("bad") -> ImportResult.Unsupported(fileName = "$uri.rar")
            uri.startsWith("lost") -> ImportResult.Failed(fileName = null)
            else -> ImportResult.Imported(bookId = 1).also { imported += uri }
        }
    }

    private fun TestScope.queue() = ImportQueue(importer, backgroundScope)

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
}
