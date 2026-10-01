package io.github.fabiann1809.reader.data.book.folder

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import io.github.fabiann1809.reader.data.book.importing.hasExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a folder the user picked with the system folder picker. */
interface FolderLister {
    /** URIs of the files in [folderUri] and its subfolders that could be books, or null if it can't be read. */
    suspend fun bookFiles(folderUri: String): List<String>?

    /** The folder's name to show the user, or null if it can't be read. */
    suspend fun name(folderUri: String): String?
}

/** [FolderLister] with the Storage Access Framework (works for local storage and cloud providers). */
class DocumentFolderLister(private val context: Context) : FolderLister {

    override suspend fun bookFiles(folderUri: String): List<String>? = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, folderUri.toUri())
        if (folder == null || !folder.canRead()) null else buildList { collect(folder, depth = 0) }
    }

    override suspend fun name(folderUri: String): String? = withContext(Dispatchers.IO) {
        DocumentFile.fromTreeUri(context, folderUri.toUri())?.name
    }

    // A shallow limit keeps a huge or looping folder tree from making the check endless.
    private fun MutableList<String>.collect(folder: DocumentFile, depth: Int) {
        for (file in folder.listFiles()) {
            when {
                file.isDirectory && depth < MAX_DEPTH -> collect(file, depth + 1)
                file.isFile && BOOK_EXTENSIONS.any { hasExtension(file.name, it) } -> add(file.uri.toString())
            }
        }
    }

    private companion object {
        const val MAX_DEPTH = 3

        // Only a cheap first filter by name; the importer checks the real content.
        val BOOK_EXTENSIONS = listOf("epub", "pdf", "txt", "cbz")
    }
}
