package io.github.fabiann1809.reader.ui.reader

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/** Shows an EPUB with Readium's EPUB navigator. */
class EpubReaderFragment : NavigatorHostFragment() {

    override val navigatorClass: Class<out Fragment> = EpubNavigatorFragment::class.java

    override fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory =
        EpubNavigatorFactory(publication).createFragmentFactory(initialLocator = initialLocator)

    override fun dummyNavigatorFactory(): FragmentFactory = EpubNavigatorFragment.createDummyFactory()
}
