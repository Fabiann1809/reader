package io.github.fabiann1809.reader.data.book.importing

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookKind
import io.github.fabiann1809.reader.data.book.BookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface ImportResult {
    data class Imported(val bookId: Long) : ImportResult

    /** The file is not an EPUB or PDF the app can read (or it is damaged). */
    data object Unsupported : ImportResult

    /** The file could not be copied (e.g. a cloud file that failed to download). */
    data object Failed : ImportResult
}

/** Adds a digital book from a file the user picked with the system file picker. */
fun interface BookImporter {
    /** [uri] is the picked document's content URI, as text so ViewModels stay free of Android types. */
    suspend fun import(uri: String): ImportResult
}

/**
 * Copies the picked file into private storage first: the picker's permission does not outlive
 * the app, and cloud providers (Drive, Dropbox) may not keep the file available offline.
 */
class DefaultBookImporter(
    private val contentResolver: ContentResolver,
    private val bookFiles: BookFiles,
    private val fileReader: BookFileReader,
    private val bookRepository: BookRepository,
    // Last-resort title (user-facing, so it comes from resources) when neither metadata nor file name help.
    private val untitled: String,
) : BookImporter {

    override suspend fun import(uri: String): ImportResult = withContext(Dispatchers.IO) {
        importFrom(Uri.parse(uri))
    }

    private suspend fun importFrom(uri: Uri): ImportResult {
        val copy = bookFiles.newFile(extension = PARTIAL_EXTENSION)
        try {
            if (!copyTo(uri, copy)) return ImportResult.Failed.also { copy.delete() }
            val info = readSafely(copy) ?: return ImportResult.Unsupported.also { copy.delete() }
            val stored = copy.withExtension(info.format.name.lowercase())
            val book = Book(
                title = info.title ?: titleFromFileName(displayName(uri)).ifEmpty { untitled },
                author = info.author.orEmpty(),
                kind = BookKind.DIGITAL,
                format = info.format,
                filePath = bookFiles.relativePath(stored),
                language = info.language,
            )
            return ImportResult.Imported(bookRepository.addBook(book))
        } catch (e: CancellationException) {
            // Never leave a half-copied file behind.
            copy.delete()
            throw e
        }
    }

    /** False when the provider cannot give the file (missing, no permission, download failed). */
    private fun copyTo(uri: Uri, target: File): Boolean = try {
        val input = contentResolver.openInputStream(uri)
        input?.use { source -> target.outputStream().use { source.copyTo(it) } } != null
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    // A damaged file can make the parsers (including native PDFium) throw; that is "unsupported", not a crash.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun readSafely(file: File): BookFileInfo? = try {
        fileReader.read(file)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun displayName(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun File.withExtension(extension: String): File {
        val target = File(parentFile, "$nameWithoutExtension.$extension")
        return if (renameTo(target)) target else this
    }

    private companion object {
        const val PARTIAL_EXTENSION = "part"
    }
}

/**
 * Title for a book whose file has no title metadata: the file name without its extension,
 * with "_" read as spaces (e.g. "el_principito.epub" → "el principito"). Empty when there is no name.
 */
fun titleFromFileName(fileName: String?): String {
    val name = fileName?.trim().orEmpty()
    val base = if ('.' in name) name.substringBeforeLast('.') else name
    return base.replace('_', ' ').trim()
}
