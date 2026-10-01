package io.github.fabiann1809.reader.data.book.importing

import android.content.ContentResolver
import android.content.Intent
import androidx.core.net.toUri

/**
 * Read access to picked files beyond the current run of the app, so a failed import can be
 * retried after reopening it. Both calls are best effort: some files (e.g. shared from other apps)
 * cannot be kept, and then a retry after a restart simply fails again.
 */
interface FileAccess {
    fun keep(uri: String)

    fun release(uri: String)
}

/** [FileAccess] with Android's persistable URI permissions. */
class PersistedFileAccess(private val contentResolver: ContentResolver) : FileAccess {

    override fun keep(uri: String) {
        try {
            contentResolver.takePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Only files picked with the system picker offer a persistable permission.
        }
    }

    override fun release(uri: String) {
        try {
            contentResolver.releasePersistableUriPermission(uri.toUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // It was never kept.
        }
    }
}
