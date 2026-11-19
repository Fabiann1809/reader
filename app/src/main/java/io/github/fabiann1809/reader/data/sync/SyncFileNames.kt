package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * What the cloud calls the files of the records. The same file has the same name on every device
 * and the local random names never leave the phone:
 * - a book's file by the book's syncId (a book's file never changes),
 * - a voice note by the note's syncId,
 * - a cover by its content, because the user can change it.
 * In a record's data, `filePath`, `coverPath` and `audioPath` hold these names, not local paths.
 */
object SyncFileNames {

    private const val BOOK_PREFIX = "book-"
    private const val COVER_PREFIX = "cover-"
    private const val VOICE_PREFIX = "voice-"
    private const val COVER_HASH_LENGTH = 24

    fun book(syncId: String, localPath: String): String = "$BOOK_PREFIX$syncId.${extensionOf(localPath, "bin")}"

    fun cover(contentHash: String): String = "$COVER_PREFIX${contentHash.take(COVER_HASH_LENGTH)}.jpg"

    fun voice(syncId: String): String = "$VOICE_PREFIX$syncId.m4a"

    /** Whether the sync made this file: it only ever deletes those from the cloud. */
    fun isManaged(name: String): Boolean = listOf(BOOK_PREFIX, COVER_PREFIX, VOICE_PREFIX).any(name::startsWith)

    fun extensionOf(name: String, fallback: String): String = name.substringAfterLast('.', fallback).ifBlank { fallback }

    /** The cloud files a record needs. */
    fun of(record: SyncRecord): List<String> {
        val fields = when (record.key.type) {
            SyncTypes.BOOK -> listOf("filePath", "coverPath")
            SyncTypes.NOTE -> listOf("audioPath")
            else -> return emptyList()
        }
        val data: JsonObject = Json.parseToJsonElement(record.data).jsonObject
        return fields.mapNotNull { data[it]?.jsonPrimitive?.contentOrNull }
    }
}
