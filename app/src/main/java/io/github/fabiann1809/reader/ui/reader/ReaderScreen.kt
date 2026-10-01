package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
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
                AndroidFragment<EpubReaderFragment>(
                    arguments = bundleOf(EpubReaderFragment.ARG_BOOK_ID to state.bookId),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is ReaderUiState.CannotOpen -> CannotOpen(state.problem, onNavigateUp)
        }
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
