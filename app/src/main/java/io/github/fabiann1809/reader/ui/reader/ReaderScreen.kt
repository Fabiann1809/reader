package io.github.fabiann1809.reader.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.data.reader.OpenProblem
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.ImmersiveMode
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.StatusMessage

/** Full-screen reading. The book's pages come from Readium's navigator, embedded as a Fragment. */
@Composable
fun ReaderScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            ReaderUiState.Loading -> CircularProgressIndicator()
            is ReaderUiState.Ready -> {
                ImmersiveMode()
                SystemBrightnessOnLeave()
                // While the controls are hidden, the first "Atrás" shows them; with them shown, it leaves.
                BackHandler(enabled = !state.controlsVisible, onBack = viewModel::showControls)
                BookNavigator(state.bookId, state.format, onCenterTap = viewModel::toggleControls)
                ReaderControls(
                    state,
                    onBack = onNavigateUp,
                    onSeek = viewModel::seekTo,
                    onBookmark = viewModel::toggleBookmark,
                    onIndex = viewModel::showContents,
                )
                if (state.contentsVisible) {
                    ContentsSheet(
                        state,
                        onChapterClick = viewModel::goToChapter,
                        onBookmarkClick = viewModel::goToBookmark,
                        onDeleteBookmark = viewModel::deleteBookmark,
                        onDismiss = viewModel::hideContents,
                    )
                }
            }
            is ReaderUiState.CannotOpen -> CannotOpen(state.problem, onNavigateUp)
        }
    }
}

/**
 * Gives the screen back the system's brightness when the reader is left (the edge drag changes it
 * while reading). On a rotation this resets the old window, which is about to go anyway.
 */
@Composable
private fun SystemBrightnessOnLeave() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        onDispose { window?.let { ReaderBrightness(it).show(null) } }
    }
}

/** Readium's navigator for the book's format. */
@Composable
private fun BookNavigator(bookId: Long, format: BookFormat, onCenterTap: () -> Unit) {
    val arguments = bundleOf(NavigatorHostFragment.ARG_BOOK_ID to bookId)
    val modifier = Modifier.fillMaxSize()
    val connect: (NavigatorHostFragment) -> Unit = { it.onCenterTap = onCenterTap }
    when (format) {
        BookFormat.PDF -> AndroidFragment<PdfReaderFragment>(arguments = arguments, modifier = modifier, onUpdate = connect)
        // The session only opens EPUB and PDF (see ReadiumReaderSession).
        else -> AndroidFragment<EpubReaderFragment>(arguments = arguments, modifier = modifier, onUpdate = connect)
    }
}

@Composable
private fun CannotOpen(problem: OpenProblem, onNavigateUp: () -> Unit) {
    val message = when (problem) {
        OpenProblem.NO_FILE -> R.string.reader_no_file
        OpenProblem.NOT_SUPPORTED_YET -> R.string.reader_not_supported_yet
        OpenProblem.UNREADABLE -> R.string.reader_unreadable
    }
    StatusMessage(
        icon = R.drawable.ic_warning_circle,
        title = stringResource(R.string.reader_cannot_open),
        message = stringResource(message),
        action = { OutlineButton(text = stringResource(R.string.book_back_to_library), onClick = onNavigateUp) },
        modifier = Modifier.safeDrawingPadding(),
    )
}
