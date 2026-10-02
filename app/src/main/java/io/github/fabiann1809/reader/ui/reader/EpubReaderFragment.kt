package io.github.fabiann1809.reader.ui.reader

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Shows an EPUB with Readium's EPUB navigator, styled with the "Aa" settings (T11.7), which apply
 * as soon as they change. A pinch changes the font size only while the book is open (T11.5).
 */
@OptIn(ExperimentalReadiumApi::class)
class EpubReaderFragment : NavigatorHostFragment() {

    override val navigatorClass: Class<out Fragment> = EpubNavigatorFragment::class.java

    override val onPinch: (Float) -> Unit = { scale ->
        val current = session.adjustments.fontSize ?: session.readingSettings.value.fontSize
        val fontSize = fontSizeAfterPinch(current, scale)
        session.adjustments = session.adjustments.copy(fontSize = fontSize)
        applyPreferences(session.readingSettings.value)
    }

    override fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory =
        EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = session.readingSettings.value.toEpubPreferences(session.adjustments.fontSize),
            configuration = readerFontsConfiguration(),
        )

    override fun dummyNavigatorFactory(): FragmentFactory = EpubNavigatorFragment.createDummyFactory()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                session.readingSettings.collect(::applyPreferences)
            }
        }
    }

    private fun applyPreferences(settings: ReadingSettings) {
        (navigator as? EpubNavigatorFragment)?.submitPreferences(settings.toEpubPreferences(session.adjustments.fontSize))
    }
}
