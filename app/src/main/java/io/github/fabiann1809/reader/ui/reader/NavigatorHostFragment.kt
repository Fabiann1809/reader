package io.github.fabiann1809.reader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.data.reader.ReaderSession
import kotlinx.coroutines.launch
import io.github.fabiann1809.reader.ui.components.isReduceMotionOn
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Hosts one of Readium's navigators, which are Fragments, so Compose can embed it (see ReaderScreen).
 * It follows Readium's guide: the navigator's FragmentFactory needs the open publication and must be
 * set before super.onCreate, because Android recreates the navigator there (e.g. on rotation).
 * Each format only says which navigator to create. Readium marks its tap events as experimental,
 * but they are its documented way to get taps on the page.
 */
@OptIn(ExperimentalReadiumApi::class)
abstract class NavigatorHostFragment : Fragment() {

    /** The navigator's class, the one Android restores by name. */
    protected abstract val navigatorClass: Class<out Fragment>

    /** Creates the navigator for [publication], starting at [initialLocator] (null for the start). */
    protected abstract fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory

    /** Restores an empty navigator after the app process was killed (Readium's "dummy" factory). */
    protected abstract fun dummyNavigatorFactory(): FragmentFactory

    /** What a pinch does on this format, or null when its navigator handles pinches itself. */
    protected open val onPinch: ((Float) -> Unit)? = null

    /** Set by ReaderScreen: a tap in the middle of the page shows or hides the controls. */
    var onCenterTap: () -> Unit = {}

    private var brightness: ReaderBrightness? = null
    private var pageTurns: DirectionalNavigationAdapter? = null

    /** The navigator, once it was added (null after the process was killed, until the book reopens). */
    protected val navigator: VisualNavigator?
        get() = childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? VisualNavigator

    private val bookId: Long
        get() = requireArguments().getLong(ARG_BOOK_ID)

    protected val session: ReaderSession
        get() = (requireActivity().application as ReaderApplication).container.readerSession

    override fun onCreate(savedInstanceState: Bundle?) {
        val publication = session.publication(bookId)
        // After the app process was killed nothing is open yet: an empty navigator is restored, and
        // ReaderScreen adds this fragment again once the book is reopened.
        childFragmentManager.fragmentFactory = if (publication == null) {
            dummyNavigatorFactory()
        } else {
            // Starts where the reader left off (saved in the book, or the last page turn before a rotation).
            navigatorFactory(publication, session.initialLocator(bookId))
        }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && publication != null) {
            childFragmentManager.commitNow { add(CONTAINER_ID, navigatorClass, Bundle(), NAVIGATOR_TAG) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ReaderGestureLayout(inflater.context).apply {
            addView(FragmentContainerView(inflater.context).apply { id = CONTAINER_ID })
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = navigator ?: return
        // Order matters: the first listener that handles a tap wins.
        navigator.addInputListener(CenterTapListener(view))
        // Taps on the left or right 30 % turn the page, in the book's reading direction (design 03 §4).
        animatePageTurns(true)
        val brightness = ReaderBrightness(requireActivity().window).also { brightness = it }
        brightness.show(session.adjustments.brightness)
        (view as ReaderGestureLayout).apply {
            onEdgeDrag = { drag -> adjustBrightness(brightness, drag) }
            onPinch = this@NavigatorHostFragment.onPinch
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Each page turn is reported, so reopening the book returns to the same page (T11.2).
                launch { navigator.currentLocator.collect { session.reportLocation(bookId, it) } }
                launch { session.jumps.collect { navigator.go(it) } }
            }
        }
    }

    /**
     * Whether a tap on the side slides to the next page or shows it at once (T11.8). Reduce
     * Motion always shows it at once.
     */
    protected fun animatePageTurns(animated: Boolean) {
        val navigator = navigator ?: return
        val overflowable = navigator as? OverflowableNavigator ?: return
        pageTurns?.let(navigator::removeInputListener)
        val adapter = DirectionalNavigationAdapter(overflowable, animatedTransition = animated && !isReduceMotionOn(requireContext()))
        navigator.addInputListener(adapter)
        pageTurns = adapter
    }

    private fun adjustBrightness(brightness: ReaderBrightness, dragFraction: Float) {
        val value = brightness.afterDrag(session.adjustments.brightness, dragFraction)
        session.adjustments = session.adjustments.copy(brightness = value)
        brightness.show(value)
    }

    // Fragments only apply the brightness: after a rotation Android may create and drop an extra
    // reader, so giving the system's back is ReaderScreen's job, when the reader is left.
    override fun onDestroyView() {
        brightness = null
        pageTurns = null
        super.onDestroyView()
    }

    /** Taps in the middle of the page go to [onCenterTap]; the others stay with Readium. */
    private inner class CenterTapListener(private val page: View) : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            if (!isCenterTap(event.point.x, page.width)) return false
            onCenterTap()
            return true
        }
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "navigator"
        private val CONTAINER_ID = R.id.reader_navigator_container
    }
}
