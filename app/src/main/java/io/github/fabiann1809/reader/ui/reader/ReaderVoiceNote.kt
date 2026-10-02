package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.voice.VoiceRecordingSheet
import io.github.fabiann1809.reader.ui.voice.VoiceRecordingUiState
import io.github.fabiann1809.reader.ui.voice.VoiceRecordingViewModel

/**
 * "Grabar" in the reader (T13.3): the recording sheet while [state] asks for it, and once the
 * note is saved in the book at the current page, a snackbar to undo it.
 */
@Composable
fun ReaderVoiceNote(
    state: ReaderUiState.Ready,
    currentLocation: () -> String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceRecordingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val voiceState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(voiceState) {
        val saved = voiceState as? VoiceRecordingUiState.Saved ?: return@LaunchedEffect
        onClose()
        val noteId = saved.noteId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(R.string.voice_saved),
            actionLabel = resources.getString(R.string.voice_undo),
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoSave(noteId) else viewModel.savedShown()
    }

    if (state.recordingVoice && voiceState !is VoiceRecordingUiState.Saved) {
        val position = state.position
        VoiceRecordingSheet(
            viewModel = viewModel,
            subtitle = if (position != null) stringResource(R.string.voice_book_page, state.title, position) else state.title,
            onSave = { viewModel.save(state.bookId, currentLocation(), position) },
            onDismiss = onClose,
        )
    }
    Box(modifier = modifier.navigationBarsPadding()) {
        SnackbarHost(snackbarHostState)
    }
}
