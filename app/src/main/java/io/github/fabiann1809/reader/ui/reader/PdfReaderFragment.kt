package io.github.fabiann1809.reader.ui.reader

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.io.File
import java.io.IOException

/**
 * Shows a PDF with Readium's PDF navigator drawn by PDFium, the same engine that reads PDF
 * metadata on import. Its positions are Locators too, so saving the page works as for EPUB.
 */
@OptIn(ExperimentalReadiumApi::class)
class PdfReaderFragment : NavigatorHostFragment() {

    private val engine = PdfiumEngineProvider()

    override val navigatorClass: Class<out Fragment> = PdfNavigatorFragment::class.java

    override fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory =
        PdfNavigatorFactory(publication, engine).createFragmentFactory(initialLocator = initialLocator)

    override fun dummyNavigatorFactory(): FragmentFactory = PdfNavigatorFragment.createDummyFactory(engine)

    /**
     * Saves what the page shows inside [zone] as an image for the OCR (T11.14): PDFium draws pages
     * as pictures, so there is no text to select. Returns the image's URI, or null if it failed.
     * Only the page is drawn, never the marking on top of it.
     */
    suspend fun captureZone(zone: PageZone): String? {
        val page = view ?: return null
        val snapshot = page.drawToBitmap()
        val left = zone.left.toInt().coerceIn(0, snapshot.width - 1)
        val top = zone.top.toInt().coerceIn(0, snapshot.height - 1)
        val width = zone.width.toInt().coerceIn(1, snapshot.width - left)
        val height = zone.height.toInt().coerceIn(1, snapshot.height - top)
        val crop = Bitmap.createBitmap(snapshot, left, top, width, height)
        val file = File(requireContext().cacheDir, ZONE_FILE)
        return withContext(Dispatchers.IO) {
            try {
                file.outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
                Uri.fromFile(file).toString()
            } catch (e: IOException) {
                null
            }
        }
    }

    private companion object {
        // One file, replaced each time: it is only needed until the OCR has read it.
        const val ZONE_FILE = "reader-zone.png"
        const val PNG_QUALITY = 100
    }
}
