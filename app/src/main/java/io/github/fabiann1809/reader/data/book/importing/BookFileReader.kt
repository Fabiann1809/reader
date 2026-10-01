package io.github.fabiann1809.reader.data.book.importing

import android.graphics.Bitmap
import android.util.Size
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.coverFitting
import org.readium.r2.shared.util.use
import java.io.File

/** What the app needs from a book file when importing it. Each field is null when the file does not have it. */
data class BookFileInfo(
    val format: BookFormat,
    val title: String?,
    val author: String?,
    val language: String?,
    /** Already scaled to fit [MaxCoverSize]. */
    val cover: Bitmap? = null,
)

/**
 * Reads the format and metadata of a book file. [fileName] is the name the user's file had
 * (null if unknown). Returns null when the file is not a book the app supports.
 */
fun interface BookFileReader {
    suspend fun read(file: File, fileName: String?): BookFileInfo?
}

/**
 * [BookFileReader] for EPUB, PDF (through the PDFium adapter) and CBZ with Readium, and for TXT,
 * which Readium does not handle, by itself.
 */
class ReadiumBookFileReader(private val readium: ReadiumToolkit) : BookFileReader {

    override suspend fun read(file: File, fileName: String?): BookFileInfo? =
        readPublication(file) ?: readPlainText(file, fileName)

    // Plain text has no format of its own to recognize, so it needs both the .txt name and text content
    // (content alone would also accept HTML, CSV, logs...). Its title comes from the file name.
    private fun readPlainText(file: File, fileName: String?): BookFileInfo? =
        if (hasExtension(fileName, "txt") && isPlainTextFile(file)) {
            BookFileInfo(format = BookFormat.TXT, title = null, author = null, language = null)
        } else {
            null
        }

    private suspend fun readPublication(file: File): BookFileInfo? {
        // Readium looks at the content, not the extension, so a renamed file is still recognized.
        val asset = readium.asset(file) ?: return null
        val format = readium.bookFormat(asset)
        if (format == null) {
            asset.close()
            return null
        }
        val publication = readium.open(asset) ?: return null
        // The publication owns the asset from here and closes it.
        return publication.use { it.toInfo(format, cover = cover(it, format, file)) }
    }

    // EPUB: the cover image the book declares. CBZ: its first image. PDF: the first page, rendered
    // with PdfRenderer on white because PDFium draws pages on a transparent bitmap.
    private suspend fun cover(publication: Publication, format: BookFormat, file: File): Bitmap? = when (format) {
        BookFormat.PDF -> renderPdfCover(file)
        else -> publication.coverFitting(Size(MaxCoverSize.width, MaxCoverSize.height))
    }

    private fun Publication.toInfo(format: BookFormat, cover: Bitmap?) = BookFileInfo(
        format = format,
        title = metadata.title?.trim()?.ifEmpty { null },
        author = metadata.authors.map { it.name.trim() }.filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { null },
        language = metadata.languages.firstOrNull(),
        cover = cover,
    )
}
