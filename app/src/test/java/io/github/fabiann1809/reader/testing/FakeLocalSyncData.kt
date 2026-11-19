package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.sync.FileDownloader
import io.github.fabiann1809.reader.data.sync.LocalSnapshot
import io.github.fabiann1809.reader.data.sync.LocalSyncData
import io.github.fabiann1809.reader.data.sync.SyncFileNames
import io.github.fabiann1809.reader.data.sync.merge.Deletion
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import java.io.File

/** One device's data in memory: [records], [deletions] and the local [files] by cloud name. [folder] holds the files. */
class FakeLocalSyncData(private val folder: File) : LocalSyncData {

    val records = mutableMapOf<RecordKey, SyncRecord>()
    val deletions = mutableMapOf<RecordKey, Long>()
    val files = mutableMapOf<String, File>()

    fun put(record: SyncRecord, fileContents: Map<String, String> = emptyMap()) {
        records[record.key] = record
        deletions.remove(record.key)
        fileContents.forEach { (name, text) -> files[name] = File(folder, name).apply { writeText(text) } }
    }

    /** What deleting does in the app: the record goes and a tombstone stays. */
    fun delete(key: RecordKey, at: Long) {
        records.remove(key)
        deletions[key] = at
    }

    fun fileText(name: String): String? = files[name]?.readText()

    override suspend fun snapshot() = LocalSnapshot(
        SyncSnapshot(records.values.toList(), deletions.map { Deletion(it.key, it.value) }),
        files.toMap(),
    )

    override suspend fun rename(renames: Map<RecordKey, String>) {
        renames.forEach { (key, syncId) ->
            val record = records.remove(key) ?: return@forEach
            val newKey = RecordKey(key.type, syncId)
            records[newKey] = record.copy(key = newKey)
        }
    }

    override suspend fun apply(toApply: List<SyncRecord>, toDelete: List<RecordKey>, downloader: FileDownloader) {
        // All or nothing: the files come first, and if one fails nothing has changed.
        val arrived = mutableMapOf<String, File>()
        toApply.flatMap(SyncFileNames::of).filter { it !in files }.distinct().forEach { name ->
            val into = File(folder, name)
            downloader.download(name, into)
            arrived[name] = into
        }
        files.putAll(arrived)
        toDelete.forEach { records.remove(it) }
        toApply.forEach { records[it.key] = it }
    }
}
