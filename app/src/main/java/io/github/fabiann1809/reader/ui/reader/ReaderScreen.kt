package io.github.fabiann1809.reader.ui.reader

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

/** Full-screen reading. The book's pages come from Readium's navigator, embedded as a Fragment. */
@Composable
fun ReaderScreen(
    onNavigateUp: () -> Unit,
    // "Nota" on a selection: writes a note on that passage, anchored to its place (T11.13).
    onWriteNote: (sourceText: String, location: String) -> Unit,
    // "Crear ficha" on a selection or an explanation (T14.2): the text and the page it is on.
    onCreateFlashcard: (source: String, page: Int?) -> Unit = { _, _ -> },
    // "Ahora tú" after an explanation (T15.1).
    onNowYou: (source: String) -> Unit = {},
    viewModel: ReaderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground(uiState)),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            ReaderUiState.Loading -> CircularProgressIndicator()
            is ReaderUiState.Ready -> {
                ImmersiveMode()
                SystemBrightnessOnLeave()
                // While the controls are hidden, the first "Atrás" shows them; with them shown, it leaves.
                BackHandler(enabled = !state.controlsVisible, onBack = viewModel::showControls)
                KeepScreenOn(state.readingSettings.keepScreenOn)
                val indicators = state.readingSettings.showsIndicators
                // The page ends above the indicators, so they never cover text.
                val pageModifier = Modifier.padding(bottom = if (indicators) IndicatorsHeight else 0.dp)
                // Kept so the selection capsule can end the selection, and a PDF zone can be captured.
                var epubFragment by remember { mutableStateOf<EpubReaderFragment?>(null) }
                var pdfFragment by remember { mutableStateOf<PdfReaderFragment?>(null) }
                val scope = rememberCoroutineScope()
                BookNavigator(
                    state.bookId,
                    state.format,
                    onCenterTap = viewModel::toggleControls,
                    onSelection = viewModel::setSelection,
                    onEpubFragment = { epubFragment = it },
                    onPdfFragment = { pdfFragment = it },
                    modifier = pageModifier,
                )
                if (indicators) {
                    ReadingIndicators(
                        settings = state.readingSettings,
                        position = state.position,
                        positionCount = state.positionCount,
                        textColor = pageTextColor(state),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                state.selection?.let { selection ->
                    ReaderSelection(
                        selection,
                        darkPage = state.readingSettings.pageColors().isDark,
                        onExplain = viewModel::explainSelection,
                        onHighlight = viewModel::highlightSelection,
                        onNote = { onWriteNote(selection.text, selection.location) },
                        onCard = { onCreateFlashcard(selection.text, state.position) },
                        onVoiceNote = viewModel::startVoiceNote,
                        onEnd = { epubFragment?.clearSelection() },
                        modifier = pageModifier,
                    )
                }
                ReaderControls(
                    state,
                    onBack = onNavigateUp,
                    onSeek = viewModel::seekTo,
                    onBookmark = viewModel::toggleBookmark,
                    onIndex = viewModel::showContents,
                    onTextSettings = viewModel::showTextSettings,
                    // In a PDF, "IA" marks a zone to explain (T11.14); EPUB text is selected instead.
                    onAi = if (state.format == BookFormat.PDF) viewModel::startZonePicking else ({}),
                    onRecord = viewModel::startVoiceNote,
                )
                state.zonePicking?.let { picking ->
                    ZonePicker(
                        state = picking,
                        onExplain = { zone -> scope.launch { viewModel.explainZone(pdfFragment?.captureZone(zone)) } },
                        onCancel = viewModel::cancelZonePicking,
                        modifier = pageModifier,
                    )
                }
                if (state.textSettingsVisible) {
                    ReadingSettingsSheet(
                        settings = state.readingSettings,
                        isPdf = state.format == BookFormat.PDF,
                        onChange = viewModel::updateReadingSettings,
                        onDismiss = viewModel::hideTextSettings,
                    )
                }
                ReaderVoiceNote(
                    state,
                    currentLocation = viewModel::currentLocation,
                    onClose = viewModel::closeVoiceNote,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                state.explaining?.let { text ->
                    ReaderExplanation(
                        state.bookId,
                        text,
                        onDismiss = viewModel::closeExplanation,
                        onCreateFlashcard = { source ->
                            viewModel.closeExplanation()
                            onCreateFlashcard(source, state.position)
                        },
                        onNowYou = { source ->
                            viewModel.closeExplanation()
                            onNowYou(source)
                        },
                    )
                }
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

/** Around an EPUB's page, its theme's color, so the edges match the page; the app's surface otherwise. */
@Composable
private fun pageBackground(state: ReaderUiState): Color =
    if (state is ReaderUiState.Ready && state.format == BookFormat.EPUB) {
        Color(state.readingSettings.pageColors().background)
    } else {
        MaterialTheme.colorScheme.surface
    }

/** The page's text color: the EPUB theme's, or the app's for PDFs. */
@Composable
private fun pageTextColor(state: ReaderUiState.Ready): Color =
    if (state.format == BookFormat.EPUB) Color(state.readingSettings.pageColors().text) else MaterialTheme.colorScheme.onSurface

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
private fun BookNavigator(
    bookId: Long,
    format: BookFormat,
    onCenterTap: () -> Unit,
    onSelection: (TextSelection?) -> Unit,
    onEpubFragment: (EpubReaderFragment) -> Unit,
    onPdfFragment: (PdfReaderFragment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val arguments = bundleOf(NavigatorHostFragment.ARG_BOOK_ID to bookId)
    val fragmentModifier = modifier.fillMaxSize()
    val connect: (NavigatorHostFragment) -> Unit = { fragment ->
        fragment.onCenterTap = onCenterTap
        // Only EPUB pages have selectable text; PDFs get it with OCR later (T11.14).
        (fragment as? EpubReaderFragment)?.let { epub ->
            epub.onSelection = onSelection
            onEpubFragment(epub)
        }
        (fragment as? PdfReaderFragment)?.let(onPdfFragment)
    }
    when (format) {
        BookFormat.PDF -> AndroidFragment<PdfReaderFragment>(arguments = arguments, modifier = fragmentModifier, onUpdate = connect)
        // The session only opens EPUB and PDF (see ReadiumReaderSession).
        else -> AndroidFragment<EpubReaderFragment>(arguments = arguments, modifier = fragmentModifier, onUpdate = connect)
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
