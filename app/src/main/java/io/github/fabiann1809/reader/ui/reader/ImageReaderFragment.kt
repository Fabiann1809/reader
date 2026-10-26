package io.github.fabiann1809.reader.ui.reader

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/** Shows a CBZ (a ZIP of page images) with Readium's image navigator, one image per page. */
@OptIn(ExperimentalReadiumApi::class)
class ImageReaderFragment : NavigatorHostFragment() {

    override val navigatorClass: Class<out Fragment> = ImageNavigatorFragment::class.java

    override fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory =
        ImageNavigatorFragment.createFactory(publication, initialLocator)

    override fun dummyNavigatorFactory(): FragmentFactory = ImageNavigatorFragment.createDummyFactory()
}
