package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.data.sync.merge.Deletion
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SyncManifestTest {

    private val snapshot = SyncSnapshot(
        records = listOf(
            SyncRecord(
                key = RecordKey("note", "n1"),
                updatedAt = 10,
                data = """{"content":"Idea with \"quotes\" and ñ"}""",
                parents = listOf(RecordKey("book", "b1")),
            ),
        ),
        deletions = listOf(Deletion(RecordKey("book", "b2"), 20)),
    )

    @Test
    fun aSnapshotSurvivesEncodingAndDecoding() {
        assertEquals(snapshot, SyncManifest.decode(SyncManifest.encode(snapshot)))
    }

    @Test
    fun aNewerFormatIsRefusedInsteadOfBeingMisread() {
        val newer = """{"formatVersion":${SyncManifest.CURRENT_FORMAT + 1},"snapshot":{}}"""

        assertThrows(SyncError.NewerFormat::class.java) { SyncManifest.decode(newer) }
    }

    @Test
    fun unknownFieldsFromALaterMinorChangeAreIgnored() {
        val withExtra = SyncManifest.encode(snapshot).replaceFirst("{", """{"somethingNew":true,""")

        assertEquals(snapshot, SyncManifest.decode(withExtra))
    }

    @Test
    fun garbageIsReportedAsUnreadable() {
        assertThrows(SyncError.Unknown::class.java) { SyncManifest.decode("not json") }
    }
}
