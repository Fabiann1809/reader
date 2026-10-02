package io.github.fabiann1809.reader.ui.reader

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.fabiann1809.reader.data.prefs.PageEffect
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

    /** Set by ReaderScreen: the text selected on the page, or null when the selection ends (T11.10). */
    var onSelection: (TextSelection?) -> Unit = {}

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
            configuration = readerFontsConfiguration().apply { selectionActionModeCallback = SelectionCallback() },
        )

    override fun dummyNavigatorFactory(): FragmentFactory = EpubNavigatorFragment.createDummyFactory()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { session.readingSettings.collect(::applyPreferences) }
                // Readium redraws only what changed in the group (T11.12).
                launch {
                    session.highlights.collect { highlights ->
                        (navigator as? EpubNavigatorFragment)?.applyDecorations(highlights.mapNotNull { it.toDecoration() }, HIGHLIGHTS_GROUP)
                    }
                }
            }
        }
    }

    /** Ends the selection, e.g. once a capsule action used it. */
    fun clearSelection() {
        (navigator as? EpubNavigatorFragment)?.clearSelection()
    }

    /**
     * Replaces the system's text menu with the app's capsule: the menu stays empty (so Android shows
     * none) while ReaderScreen draws the capsule from [onSelection].
     */
    private inner class SelectionCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            reportSelection()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            // Dragging the handles changes the selection: the capsule follows it.
            reportSelection()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode) = onSelection(null)
    }

    private fun reportSelection() {
        val navigator = navigator as? EpubNavigatorFragment ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val selection = navigator.currentSelection() ?: return@launch
            val text = selection.locator.text.highlight?.takeIf { it.isNotBlank() } ?: return@launch
            val bounds = selection.rect?.let { SelectionBounds(it.left, it.top, it.right, it.bottom) }
            onSelection(TextSelection(text, bounds, selection.locator.toJSON().toString()))
        }
    }

    private fun applyPreferences(settings: ReadingSettings) {
        (navigator as? EpubNavigatorFragment)?.submitPreferences(settings.toEpubPreferences(session.adjustments.fontSize))
        // "Ninguno" turns pages at once; scrolling has no page turns to animate.
        animatePageTurns(settings.pageEffect == PageEffect.SLIDE)
    }
}
