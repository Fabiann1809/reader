package io.github.fabiann1809.reader.data.book

import android.content.Context
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.format.Specification
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

/**
 * The Readium objects that read book files, set up once and shared by the importer and the reader
 * (one per app, see AppContainer). EPUB natively, PDF through the PDFium adapter, CBZ as images.
 */
class ReadiumToolkit(context: Context) {

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

    /** The file's format, recognized by its content (not its extension). Null if Readium doesn't know it. */
    suspend fun asset(file: File): Asset? = assetRetriever.retrieve(file).getOrElse { null }

    /** Which of the app's formats [asset] is, or null for anything else (TXT is not a Readium format). */
    fun bookFormat(asset: Asset): BookFormat? = when {
        asset.format.conformsTo(Specification.Epub) -> BookFormat.EPUB
        asset.format.conformsTo(Specification.Pdf) -> BookFormat.PDF
        // A ZIP of images; comic RAR files (CBR) are not supported (see the spec's risks).
        asset.format.conformsToAll(listOf(Specification.Zip, Specification.InformalComic)) -> BookFormat.CBZ
        else -> null
    }

    /**
     * Opens [asset] as a publication, never prompting the user (e.g. for a DRM passphrase). The
     * publication then owns the asset and closes it; on failure the asset is closed here.
     */
    suspend fun open(asset: Asset): Publication? =
        publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
            asset.close()
            null
        }
}
