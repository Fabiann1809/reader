package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import io.github.fabiann1809.reader.testing.FakeLocalSyncData
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The orchestrator with two simulated devices that share one fake cloud. */
class DefaultSyncRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val cloud = FakeCloudStorage()
    private val deviceA by lazy { FakeLocalSyncData(tmp.newFolder("a")) }
    private val deviceB by lazy { FakeLocalSyncData(tmp.newFolder("b")) }
    private val syncA by lazy { DefaultSyncRepository(deviceA, cloud) }
    private val syncB by lazy { DefaultSyncRepository(deviceB, cloud) }

    private fun note(syncId: String, updatedAt: Long, text: String = "v$updatedAt") = SyncRecord(
        key = RecordKey(SyncTypes.NOTE, syncId),
        updatedAt = updatedAt,
        data = JsonObject(mapOf("content" to JsonPrimitive(text), "audioPath" to JsonNull)).toString(),
        parents = listOf(RecordKey(SyncTypes.BOOK, "book")),
    )

    /** A book record that names these cloud files. */
    private fun book(syncId: String, updatedAt: Long, file: String?, cover: String? = null, matchKey: String? = null) = SyncRecord(
        key = RecordKey(SyncTypes.BOOK, syncId),
        updatedAt = updatedAt,
        data = JsonObject(
            mapOf(
                "filePath" to (file?.let(::JsonPrimitive) ?: JsonNull),
                "coverPath" to (cover?.let(::JsonPrimitive) ?: JsonNull),
            ),
        ).toString(),
        matchKey = matchKey,
    )

    private fun keys(device: FakeLocalSyncData) = device.records.keys.map { it.syncId }.toSet()

    @Test
    fun theFirstSyncUploadsTheRecordsAndTheirFiles() = runTest {
        deviceA.put(book("book", 10, "book-book.epub", "cover-x.jpg"), mapOf("book-book.epub" to "the book", "cover-x.jpg" to "pic"))

        val report = syncA.sync().getOrThrow()

        assertTrue(report.dataUploaded)
        assertEquals(2, report.filesUploaded)
        assertEquals(setOf("book-book.epub", "cover-x.jpg"), cloud.files.keys)
        assertNotNull(cloud.manifest)
    }

    @Test
    fun filesGoUpBeforeTheManifestThatNamesThem() = runTest {
        deviceA.put(book("book", 10, "book-book.epub"), mapOf("book-book.epub" to "x"))

        syncA.sync().getOrThrow()

        assertEquals(listOf("upload:book-book.epub", "manifest"), cloud.writes)
    }

    @Test
    fun theOtherDeviceReceivesRecordsAndFiles() = runTest {
        deviceA.put(book("book", 10, "book-book.epub"), mapOf("book-book.epub" to "the book"))
        deviceA.put(note("n1", 11))
        syncA.sync().getOrThrow()

        val report = syncB.sync().getOrThrow()

        assertEquals(2, report.applied)
        assertEquals(setOf("book", "n1"), keys(deviceB))
        assertEquals("the book", deviceB.fileText("book-book.epub"))
        assertFalse(report.dataUploaded)
    }

    @Test
    fun aNewerEditTravelsInBothDirections() = runTest {
        deviceA.put(note("n1", 10, "old"))
        deviceA.put(book("book", 1, null))
        syncA.sync().getOrThrow()
        syncB.sync().getOrThrow()

        deviceB.put(note("n1", 20, "edited on B"))
        syncB.sync().getOrThrow()
        syncA.sync().getOrThrow()

        assertEquals(20L, deviceA.records.getValue(RecordKey(SyncTypes.NOTE, "n1")).updatedAt)
        assertTrue(deviceA.records.getValue(RecordKey(SyncTypes.NOTE, "n1")).data.contains("edited on B"))
    }

    @Test
    fun aDeletionReachesTheOtherDevice() = runTest {
        deviceA.put(book("book", 1, null))
        deviceA.put(note("n1", 10))
        syncA.sync().getOrThrow()
        syncB.sync().getOrThrow()

        deviceB.delete(RecordKey(SyncTypes.NOTE, "n1"), at = 30)
        syncB.sync().getOrThrow()
        val report = syncA.sync().getOrThrow()

        assertEquals(1, report.deleted)
        assertEquals(setOf("book"), keys(deviceA))
    }

    @Test
    fun deletingABookRemovesItsFilesFromTheCloud() = runTest {
        deviceA.put(book("book", 1, "book-book.epub", "cover-x.jpg"), mapOf("book-book.epub" to "b", "cover-x.jpg" to "c"))
        syncA.sync().getOrThrow()

        deviceA.delete(RecordKey(SyncTypes.BOOK, "book"), at = 50)
        syncA.sync().getOrThrow()

        assertTrue(cloud.files.isEmpty())
    }

    @Test
    fun aChangedCoverReplacesTheOldFile() = runTest {
        deviceA.put(book("book", 1, null, "cover-old.jpg"), mapOf("cover-old.jpg" to "old"))
        syncA.sync().getOrThrow()

        deviceA.put(book("book", 5, null, "cover-new.jpg"), mapOf("cover-new.jpg" to "new"))
        syncA.sync().getOrThrow()

        assertEquals(setOf("cover-new.jpg"), cloud.files.keys)
    }

    @Test
    fun aFileNobodyNamedYetIsLeftAloneBecauseAnotherDeviceMayBeUploadingIt() = runTest {
        cloud.files["book-in-flight.epub"] = "x".toByteArray()
        deviceA.put(note("n1", 1))

        syncA.sync().getOrThrow()

        assertTrue("book-in-flight.epub" in cloud.files)
    }

    @Test
    fun syncingWithNothingToDoWritesNothing() = runTest {
        deviceA.put(book("book", 1, "book-book.epub"), mapOf("book-book.epub" to "x"))
        syncA.sync().getOrThrow()
        cloud.writes.clear()

        val report = syncA.sync().getOrThrow()

        assertEquals(SyncReport(0, 0, 0, false), report)
        assertTrue(cloud.writes.isEmpty())
    }

    @Test
    fun ifTheManifestChangesMeanwhileItMergesAgainAndKeepsBothChanges() = runTest {
        deviceA.put(note("from-a", 10))
        deviceA.put(book("book", 1, null))
        // While A is about to write, B uploads its own record.
        cloud.beforeNextManifestWrite = {
            deviceB.put(note("from-b", 11))
            deviceB.put(book("book", 1, null))
            kotlinx.coroutines.runBlocking { syncB.sync().getOrThrow() }
        }

        syncA.sync().getOrThrow()
        syncB.sync().getOrThrow()

        assertEquals(setOf("book", "from-a", "from-b"), keys(deviceA))
        assertEquals(keys(deviceA), keys(deviceB))
    }

    @Test
    fun aFailedDownloadLeavesThisDeviceUntouched() = runTest {
        deviceA.put(book("book", 1, "book-book.epub"), mapOf("book-book.epub" to "x"))
        syncA.sync().getOrThrow()
        cloud.files.clear() // the file is gone from the cloud

        val result = syncB.sync()

        assertTrue(result.isFailure)
        assertTrue(deviceB.records.isEmpty())
    }

    @Test
    fun cloudErrorsComeOutAsSyncErrorsAndChangeNothingHere() = runTest {
        deviceA.put(note("n1", 1))
        cloud.failWith = SyncError.NotAuthorized()

        val result = syncA.sync()

        assertTrue(result.exceptionOrNull() is SyncError.NotAuthorized)
        assertEquals(setOf("n1"), keys(deviceA))
        assertNull(cloud.manifest)
    }

    @Test
    fun theSameBookImportedOnBothDevicesEndsUpAsOne() = runTest {
        deviceA.put(book("book-a", 10, "book-book-a.epub", matchKey = "hash"), mapOf("book-book-a.epub" to "same"))
        deviceB.put(book("book-b", 20, "book-book-b.epub", matchKey = "hash"), mapOf("book-book-b.epub" to "same"))
        syncA.sync().getOrThrow()

        syncB.sync().getOrThrow()
        syncA.sync().getOrThrow()

        assertEquals(setOf("book-a"), keys(deviceB))
        assertEquals(setOf("book-a"), keys(deviceA))
    }
}
