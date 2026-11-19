package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.bookmark.Bookmark
import io.github.fabiann1809.reader.data.collection.BookCollectionCrossRef
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.flashcard.Flashcard
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.session.ReadingSession
import io.github.fabiann1809.reader.data.sync.merge.RecordKey
import io.github.fabiann1809.reader.data.sync.merge.SyncRecord
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Turns the app's rows into [SyncRecord]s and back. A record leaves out what is only true on this
 * device: the local `id`s (a record's parents are named by syncId instead) and the paths of files
 * (the caller gives the cloud names going out, and the local paths coming in).
 */
object RecordMapper {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // ---- Out: row to record ----

    fun record(book: Book, fileName: String?, coverName: String?) = toRecord(
        type = SyncTypes.BOOK,
        syncId = book.syncId,
        updatedAt = book.updatedAt,
        serializer = Book.serializer(),
        value = book,
        overrides = mapOf("filePath" to fileName, "coverPath" to coverName),
        matchKey = book.fileHash,
    )

    fun record(collection: Collection) = toRecord(
        SyncTypes.COLLECTION, collection.syncId, collection.updatedAt, Collection.serializer(), collection,
        // Two collections with the same name on two devices are the same one.
        matchKey = collection.name.trim().lowercase().ifEmpty { null },
    )

    fun record(note: Note, bookSyncId: String, audioName: String?) = toRecord(
        SyncTypes.NOTE, note.syncId, note.updatedAt, Note.serializer(), note,
        overrides = mapOf("audioPath" to audioName), parents = listOf(RecordKey(SyncTypes.BOOK, bookSyncId)),
    )

    fun record(bookmark: Bookmark, bookSyncId: String) = toRecord(
        SyncTypes.BOOKMARK, bookmark.syncId, bookmark.updatedAt, Bookmark.serializer(), bookmark,
        parents = listOf(RecordKey(SyncTypes.BOOK, bookSyncId)),
    )

    fun record(highlight: Highlight, bookSyncId: String) = toRecord(
        SyncTypes.HIGHLIGHT, highlight.syncId, highlight.updatedAt, Highlight.serializer(), highlight,
        parents = listOf(RecordKey(SyncTypes.BOOK, bookSyncId)),
    )

    fun record(flashcard: Flashcard, bookSyncId: String) = toRecord(
        SyncTypes.FLASHCARD, flashcard.syncId, flashcard.updatedAt, Flashcard.serializer(), flashcard,
        parents = listOf(RecordKey(SyncTypes.BOOK, bookSyncId)),
    )

    fun record(session: ReadingSession, bookSyncId: String) = toRecord(
        SyncTypes.SESSION, session.syncId, session.updatedAt, ReadingSession.serializer(), session,
        parents = listOf(RecordKey(SyncTypes.BOOK, bookSyncId)),
    )

    fun record(link: BookCollectionCrossRef, bookSyncId: String, collectionSyncId: String) = SyncRecord(
        key = RecordKey(SyncTypes.LINK, linkId(bookSyncId, collectionSyncId)),
        updatedAt = link.addedAt,
        data = JsonObject(mapOf("addedAt" to JsonPrimitive(link.addedAt))).toString(),
        parents = listOf(RecordKey(SyncTypes.BOOK, bookSyncId), RecordKey(SyncTypes.COLLECTION, collectionSyncId)),
    )

    fun linkId(bookSyncId: String, collectionSyncId: String) = "$bookSyncId|$collectionSyncId"

    /** The two syncIds a link's id is made of. */
    fun splitLinkId(linkId: String): Pair<String, String> = linkId.substringBefore('|') to linkId.substringAfter('|')

    // ---- In: record to row. [id] is the local row's id when it exists (an update), or 0 for a new row. ----

    fun book(record: SyncRecord, id: Long, filePath: String?, coverPath: String?): Book = fromRecord(
        record, Book.serializer(), "id" to JsonPrimitive(id), "filePath" to path(filePath), "coverPath" to path(coverPath),
    )

    fun collection(record: SyncRecord, id: Long): Collection = fromRecord(record, Collection.serializer(), "id" to JsonPrimitive(id))

    fun note(record: SyncRecord, id: Long, bookId: Long, audioPath: String?): Note = fromRecord(
        record, Note.serializer(), "id" to JsonPrimitive(id), "bookId" to JsonPrimitive(bookId), "audioPath" to path(audioPath),
    )

    fun bookmark(record: SyncRecord, id: Long, bookId: Long): Bookmark =
        fromRecord(record, Bookmark.serializer(), "id" to JsonPrimitive(id), "bookId" to JsonPrimitive(bookId))

    fun highlight(record: SyncRecord, id: Long, bookId: Long): Highlight =
        fromRecord(record, Highlight.serializer(), "id" to JsonPrimitive(id), "bookId" to JsonPrimitive(bookId))

    fun flashcard(record: SyncRecord, id: Long, bookId: Long): Flashcard =
        fromRecord(record, Flashcard.serializer(), "id" to JsonPrimitive(id), "bookId" to JsonPrimitive(bookId))

    fun session(record: SyncRecord, id: Long, bookId: Long): ReadingSession =
        fromRecord(record, ReadingSession.serializer(), "id" to JsonPrimitive(id), "bookId" to JsonPrimitive(bookId))

    fun link(record: SyncRecord, bookId: Long, collectionId: Long) = BookCollectionCrossRef(
        bookId = bookId,
        collectionId = collectionId,
        addedAt = json.parseToJsonElement(record.data).jsonObject.getValue("addedAt").jsonPrimitive.long,
    )

    /** What a record says a file is called in the cloud, for the field [field] (e.g. "coverPath"). */
    fun cloudName(record: SyncRecord, field: String): String? =
        (json.parseToJsonElement(record.data).jsonObject[field] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

    private fun <T> toRecord(
        type: String,
        syncId: String,
        updatedAt: Long,
        serializer: KSerializer<T>,
        value: T,
        overrides: Map<String, String?> = emptyMap(),
        matchKey: String? = null,
        parents: List<RecordKey> = emptyList(),
    ): SyncRecord {
        val fields = json.encodeToJsonElement(serializer, value).jsonObject.toMutableMap()
        // Local ids mean nothing on another device.
        listOf("id", "bookId").forEach(fields::remove)
        overrides.forEach { (field, text) -> fields[field] = path(text) }
        return SyncRecord(RecordKey(type, syncId), updatedAt, JsonObject(fields).toString(), matchKey, parents)
    }

    private fun <T> fromRecord(record: SyncRecord, serializer: KSerializer<T>, vararg local: Pair<String, JsonElement>): T {
        val fields = json.parseToJsonElement(record.data).jsonObject.toMutableMap()
        fields.putAll(local)
        return json.decodeFromJsonElement(serializer, JsonObject(fields))
    }

    private fun path(text: String?): JsonElement = if (text == null) JsonNull else JsonPrimitive(text)
}
