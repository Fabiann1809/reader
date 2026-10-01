package io.github.fabiann1809.reader.ui.reader

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Shows an EPUB with Readium's EPUB navigator. A pinch changes the font size while the book is
 * open (kept in the session; saving it comes with the "Aa" settings, T11.7).
 */
@OptIn(ExperimentalReadiumApi::class)
class EpubReaderFragment : NavigatorHostFragment() {

    override val navigatorClass: Class<out Fragment> = EpubNavigatorFragment::class.java

    override val onPinch: (Float) -> Unit = { scale ->
        val fontSize = fontSizeAfterPinch(session.adjustments.fontSize, scale)
        session.adjustments = session.adjustments.copy(fontSize = fontSize)
        (navigator as? EpubNavigatorFragment)?.submitPreferences(preferences())
    }

    override fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory =
        EpubNavigatorFactory(publication).createFragmentFactory(initialLocator = initialLocator, initialPreferences = preferences())

    override fun dummyNavigatorFactory(): FragmentFactory = EpubNavigatorFragment.createDummyFactory()

    private fun preferences() = EpubPreferences(fontSize = session.adjustments.fontSize)
}
