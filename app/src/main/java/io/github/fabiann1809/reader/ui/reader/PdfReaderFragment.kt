package io.github.fabiann1809.reader.ui.reader

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

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
}
