package io.github.fabiann1809.reader.data.sync.merge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergerTest {

    private fun key(syncId: String, type: String = "note") = RecordKey(type, syncId)

    private fun record(
        syncId: String,
        updatedAt: Long,
        data: String = "v$updatedAt",
        type: String = "note",
        matchKey: String? = null,
        parents: List<RecordKey> = emptyList(),
    ) = SyncRecord(key(syncId, type), updatedAt, data, matchKey, parents)

    private fun snapshot(records: List<SyncRecord> = emptyList(), deletions: List<Deletion> = emptyList()) =
        SyncSnapshot(records, deletions)

    private fun deletion(syncId: String, at: Long, type: String = "note") = Deletion(key(syncId, type), at)

    @Test
    fun aRecordOnlyHereIsUploaded() {
        val result = SyncMerger.merge(snapshot(listOf(record("a", 10))), snapshot())

        assertEquals(listOf(record("a", 10)), result.merged.records)
        assertTrue(result.needsUpload)
        assertTrue(result.toDelete.isEmpty())
    }

    @Test
    fun aRecordOnlyInTheCloudIsApplied() {
        val result = SyncMerger.merge(snapshot(), snapshot(listOf(record("a", 10))))

        assertEquals(listOf(record("a", 10)), result.toApply)
        assertFalse(result.needsUpload)
    }

    @Test
    fun theNewerEditWinsOnEitherSide() {
        val older = record("a", 10)
        val newer = record("a", 20)

        val cloudWins = SyncMerger.merge(snapshot(listOf(older)), snapshot(listOf(newer)))
        assertEquals(listOf(newer), cloudWins.toApply)
        assertFalse(cloudWins.needsUpload)

        val localWins = SyncMerger.merge(snapshot(listOf(newer)), snapshot(listOf(older)))
        assertTrue(localWins.toApply.isEmpty())
        assertTrue(localWins.needsUpload)
        assertEquals(listOf(newer), localWins.merged.records)
    }

    @Test
    fun identicalSidesNeedNothing() {
        val both = snapshot(listOf(record("a", 10)), listOf(deletion("b", 5)))

        val result = SyncMerger.merge(both, both)

        assertTrue(result.toApply.isEmpty())
        assertTrue(result.toDelete.isEmpty())
        assertFalse(result.needsUpload)
    }

    @Test
    fun aDeletionInTheCloudDeletesTheOlderLocalRecord() {
        val result = SyncMerger.merge(snapshot(listOf(record("a", 10))), snapshot(deletions = listOf(deletion("a", 20))))

        assertEquals(listOf(key("a")), result.toDelete)
        assertTrue(result.merged.records.isEmpty())
        assertFalse(result.needsUpload)
    }

    @Test
    fun aLocalDeletionIsUploadedAndDeletesInTheCloud() {
        val result = SyncMerger.merge(snapshot(deletions = listOf(deletion("a", 20))), snapshot(listOf(record("a", 10))))

        assertTrue(result.merged.records.isEmpty())
        assertEquals(listOf(deletion("a", 20)), result.merged.deletions)
        assertTrue(result.needsUpload)
    }

    @Test
    fun anEditAfterTheDeletionBringsTheRecordBack() {
        val result = SyncMerger.merge(snapshot(listOf(record("a", 30))), snapshot(deletions = listOf(deletion("a", 20))))

        assertEquals(listOf(record("a", 30)), result.merged.records)
        assertTrue(result.merged.deletions.isEmpty())
        assertTrue(result.needsUpload)
    }

    @Test
    fun aTieBetweenAnEditAndADeletionDeletes() {
        val result = SyncMerger.merge(snapshot(listOf(record("a", 20))), snapshot(deletions = listOf(deletion("a", 20))))

        assertTrue(result.merged.records.isEmpty())
        assertEquals(listOf(key("a")), result.toDelete)
    }

    @Test
    fun aRecordWhoseBookWasDeletedGoesWithIt() {
        val note = record("n", 50, parents = listOf(key("book", "book")))
        val local = snapshot(listOf(record("book", 10, type = "book"), note))
        val remote = snapshot(deletions = listOf(deletion("book", 40, type = "book")))

        val result = SyncMerger.merge(local, remote)

        assertTrue(result.merged.records.isEmpty())
        assertEquals(setOf(key("book", "book"), key("n")), result.toDelete.toSet())
        // The note is remembered as deleted so the cloud drops it as well.
        assertTrue(result.merged.deletions.any { it.key == key("n") })
    }

    @Test
    fun aLinkNeedsBothOfItsParents() {
        val book = record("b", 10, type = "book")
        val link = record("b|c", 10, type = "book_collection", parents = listOf(key("b", "book"), key("c", "collection")))

        val result = SyncMerger.merge(snapshot(listOf(book, link)), snapshot())

        assertEquals(listOf(book), result.merged.records)
        assertTrue(result.merged.deletions.any { it.key == link.key })
    }

    @Test
    fun theSameBookImportedOnTwoDevicesTakesTheCloudsId() {
        val local = snapshot(listOf(record("local", 10, type = "book", matchKey = "hash1")))
        val remote = snapshot(listOf(record("cloud", 5, type = "book", matchKey = "hash1")))

        assertEquals(mapOf(key("local", "book") to "cloud"), SyncMerger.findRenames(local, remote))
    }

    @Test
    fun noRenameWhenTheCloudAlreadyKnowsTheRecordOrTheHashDiffers() {
        val local = snapshot(
            listOf(record("a", 10, type = "book", matchKey = "h1"), record("b", 10, type = "book", matchKey = "h2")),
        )
        val remote = snapshot(
            listOf(record("a", 5, type = "book", matchKey = "h1"), record("c", 5, type = "book", matchKey = "h3")),
        )

        assertTrue(SyncMerger.findRenames(local, remote).isEmpty())
    }

    @Test
    fun twoLocalCopiesOfOneBookDoNotBothTakeTheSameCloudId() {
        val local = snapshot(
            listOf(record("l1", 10, type = "book", matchKey = "h"), record("l2", 10, type = "book", matchKey = "h")),
        )
        val remote = snapshot(listOf(record("cloud", 5, type = "book", matchKey = "h")))

        assertEquals(1, SyncMerger.findRenames(local, remote).size)
    }

    @Test
    fun bothDevicesConvergeToTheSameStateWhoeverSyncsFirst() {
        val a = snapshot(
            listOf(record("x", 10), record("y", 30, "edit by A"), record("z", 5)),
            listOf(deletion("w", 25)),
        )
        val b = snapshot(
            listOf(record("y", 20, "edit by B"), record("w", 10), record("v", 7)),
            listOf(deletion("z", 15)),
        )

        // A syncs first: it merges with the empty cloud, uploads, then B merges with that.
        val cloudAfterA = SyncMerger.merge(a, snapshot()).merged
        val bAfter = SyncMerger.merge(b, cloudAfterA).merged
        // Then A syncs again and sees B's result.
        val aAfter = SyncMerger.merge(a, bAfter).merged
        // The other order.
        val cloudAfterB = SyncMerger.merge(b, snapshot()).merged
        val aOther = SyncMerger.merge(a, cloudAfterB).merged

        assertEquals(bAfter, aAfter)
        assertEquals(bAfter, aOther)
        assertEquals("edit by A", bAfter.records.single { it.key == key("y") }.data)
    }

    @Test
    fun mergingTheResultAgainChangesNothing() {
        val local = snapshot(listOf(record("x", 10), record("y", 30)), listOf(deletion("w", 25)))
        val remote = snapshot(listOf(record("y", 20)), listOf(deletion("z", 15)))
        val merged = SyncMerger.merge(local, remote).merged

        val again = SyncMerger.merge(merged, merged)

        assertEquals(merged, again.merged)
        assertFalse(again.needsUpload)
        assertTrue(again.toApply.isEmpty())
        assertTrue(again.toDelete.isEmpty())
    }
}
