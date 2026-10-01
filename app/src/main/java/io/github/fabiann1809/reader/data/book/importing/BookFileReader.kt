package io.github.fabiann1809.reader.data.book.importing

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import io.github.fabiann1809.reader.data.book.BookFormat
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.coverFitting
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.use
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
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

/** Reads the format and metadata of a book file. Returns null when the file is not a book the app supports. */
fun interface BookFileReader {
    suspend fun read(file: File): BookFileInfo?
}

/** [BookFileReader] backed by Readium: EPUB natively, PDF through the PDFium adapter. */
class ReadiumBookFileReader(context: Context) : BookFileReader {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = PdfiumDocumentFactory(context),
        ),
    )

    override suspend fun read(file: File): BookFileInfo? {
        // Readium looks at the content, not the extension, so a renamed file is still recognized.
        val asset = assetRetriever.retrieve(file).getOrElse { return null }
        val format = when {
            asset.format.conformsTo(Specification.Epub) -> BookFormat.EPUB
            asset.format.conformsTo(Specification.Pdf) -> BookFormat.PDF
            else -> null
        }
        if (format == null) {
            asset.close()
            return null
        }
        // No user interaction: importing must never prompt (e.g. for a DRM passphrase).
        val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
            asset.close()
            return null
        }
        // The publication owns the asset from here and closes it.
        return publication.use { it.toInfo(format, cover = cover(it, format, file)) }
    }

    // EPUB: the cover image the book declares. PDF: the first page, rendered with PdfRenderer on white
    // because PDFium draws pages on a transparent bitmap.
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
