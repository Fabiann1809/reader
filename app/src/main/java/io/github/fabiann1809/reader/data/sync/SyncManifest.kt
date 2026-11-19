package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the manifest file holds: the agreed snapshot, and the format it is written in. */
@Serializable
data class SyncManifest(val formatVersion: Int = CURRENT_FORMAT, val snapshot: SyncSnapshot) {

    companion object {
        const val CURRENT_FORMAT = 1
        const val FILE_NAME = "manifest.json"

        private val json = Json { ignoreUnknownKeys = true }

        fun encode(snapshot: SyncSnapshot): String = json.encodeToString(serializer(), SyncManifest(snapshot = snapshot))

        /** Fails with [SyncError.NewerFormat] when a newer app wrote it, [SyncError.Unknown] when it is unreadable. */
        fun decode(content: String): SyncSnapshot {
            val manifest = try {
                json.decodeFromString(serializer(), content)
            } catch (e: SerializationException) {
                throw SyncError.Unknown("The cloud data is not readable", e)
            }
            if (manifest.formatVersion > CURRENT_FORMAT) throw SyncError.NewerFormat()
            return manifest.snapshot
        }
    }
}
