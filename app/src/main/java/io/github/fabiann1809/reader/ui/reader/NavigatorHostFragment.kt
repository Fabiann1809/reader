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
import org.readium.r2.navigator.Navigator
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Hosts one of Readium's navigators, which are Fragments, so Compose can embed it (see ReaderScreen).
 * It follows Readium's guide: the navigator's FragmentFactory needs the open publication and must be
 * set before super.onCreate, because Android recreates the navigator there (e.g. on rotation).
 * Each format only says which navigator to create.
 */
abstract class NavigatorHostFragment : Fragment() {

    /** The navigator's class, the one Android restores by name. */
    protected abstract val navigatorClass: Class<out Fragment>

    /** Creates the navigator for [publication], starting at [initialLocator] (null for the start). */
    protected abstract fun navigatorFactory(publication: Publication, initialLocator: Locator?): FragmentFactory

    /** Restores an empty navigator after the app process was killed (Readium's "dummy" factory). */
    protected abstract fun dummyNavigatorFactory(): FragmentFactory

    private val bookId: Long
        get() = requireArguments().getLong(ARG_BOOK_ID)

    private val session: ReaderSession
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
        FragmentContainerView(inflater.context).apply { id = CONTAINER_ID }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as? Navigator ?: return
        // Each page turn is reported, so reopening the book returns to the same page (T11.2).
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigator.currentLocator.collect { session.reportLocation(bookId, it) }
            }
        }
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "navigator"
        private val CONTAINER_ID = R.id.reader_navigator_container
    }
}
