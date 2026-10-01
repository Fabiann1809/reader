package io.github.fabiann1809.reader.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ReaderApplication
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment

/**
 * Hosts Readium's EPUB navigator, which is a Fragment, so Compose can embed it (see ReaderScreen).
 * It follows Readium's guide: the navigator's FragmentFactory needs the open publication and must be
 * set before super.onCreate, because Android recreates the navigator there (e.g. on rotation).
 */
class EpubReaderFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val bookId = requireArguments().getLong(ARG_BOOK_ID)
        val publication = (requireActivity().application as ReaderApplication).container.readerSession.publication(bookId)
        // After the app process was killed nothing is open yet: Readium's dummy factory restores an empty
        // navigator, and ReaderScreen adds this fragment again once the book is reopened.
        childFragmentManager.fragmentFactory = if (publication == null) {
            EpubNavigatorFragment.createDummyFactory()
        } else {
            EpubNavigatorFactory(publication).createFragmentFactory(initialLocator = null)
        }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && publication != null) {
            childFragmentManager.commitNow { add(CONTAINER_ID, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContainerView(inflater.context).apply { id = CONTAINER_ID }

    companion object {
        const val ARG_BOOK_ID = "bookId"
        private const val NAVIGATOR_TAG = "epub-navigator"
        private val CONTAINER_ID = R.id.reader_navigator_container
    }
}
