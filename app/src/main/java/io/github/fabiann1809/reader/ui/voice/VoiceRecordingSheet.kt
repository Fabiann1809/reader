package io.github.fabiann1809.reader.ui.voice

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.ui.components.PermissionStatus
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.rememberPermissionState

/**
 * The "Grabar" sheet (lámina 1i): asks for the microphone if needed, then records at once. After
 * stopping, the recording can be listened to and its transcript corrected; the check button
 * saves it ([onSave]) and closing the sheet throws it away. [subtitle] says where the note goes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordingSheet(
    viewModel: VoiceRecordingViewModel,
    subtitle: String,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val permission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tag by viewModel.tag.collectAsStateWithLifecycle()
    val granted = permission.status == PermissionStatus.GRANTED

    // Opening the sheet (or granting the microphone) starts recording right away; a note that was
    // just saved (its snackbar may still be up) doesn't stop a new one.
    LaunchedEffect(granted) {
        val current = viewModel.uiState.value
        val ready = current == VoiceRecordingUiState.Idle || current is VoiceRecordingUiState.Saved
        if (granted && ready) viewModel.start()
    }
    // Without a foreground service the microphone goes silent in the background: end the recording there.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stop() }

    val close = {
        viewModel.discard()
        onDismiss()
    }
    ModalBottomSheet(onDismissRequest = close) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
        ) {
            if (granted) {
                RecordingContent(
                    state,
                    subtitle,
                    tag,
                    onToggleTag = viewModel::toggleTag,
                    onSave = onSave,
                    onStop = viewModel::stop,
                    onTogglePlayback = viewModel::togglePlayback,
                    onRetry = viewModel::start,
                    onDiscard = close,
                    onTranscriptChange = viewModel::onTranscriptChange,
                    onRetryTranscript = viewModel::retryTranscription,
                )
            } else {
                MicrophonePermissionContent(permission.status, onRequest = permission.request)
            }
        }
    }
}

@Composable
private fun RecordingContent(
    state: VoiceRecordingUiState,
    subtitle: String,
    tag: NoteTag?,
    onToggleTag: (NoteTag) -> Unit,
    onSave: () -> Unit,
    onStop: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onTranscriptChange: (String) -> Unit,
    onRetryTranscript: () -> Unit,
) {
    val title = when (state) {
        is VoiceRecordingUiState.Recorded -> R.string.voice_recorded_title
        is VoiceRecordingUiState.Failed -> R.string.voice_failed_title
        else -> R.string.voice_recording_title
    }
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
    )
    if (state is VoiceRecordingUiState.Recording) VoiceWave(state.levels, modifier = Modifier.padding(bottom = 12.dp))
    val message = when (state) {
        is VoiceRecordingUiState.Recording -> formatDuration(state.elapsedMillis)
        is VoiceRecordingUiState.Recorded -> formatDuration(state.durationMillis)
        is VoiceRecordingUiState.Failed -> stringResource(state.reason.message())
        else -> formatDuration(0)
    }
    Text(
        text = message,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 20.dp),
    )
    val recordingOrRecorded = state is VoiceRecordingUiState.Recording || state is VoiceRecordingUiState.Recorded
    if (recordingOrRecorded) TagChips(tag, onToggle = onToggleTag, modifier = Modifier.padding(bottom = 24.dp))
    if (state is VoiceRecordingUiState.Recorded) {
        TranscriptField(
            state.transcript,
            onChange = onTranscriptChange,
            onRetry = onRetryTranscript,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }
    val canSave = (state as? VoiceRecordingUiState.Recorded)?.canSave == true
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDiscard, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_trash),
                contentDescription = stringResource(R.string.voice_discard),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (state) {
            is VoiceRecordingUiState.Recording ->
                MainButton(R.drawable.ic_stop_fill, R.string.voice_stop, MaterialTheme.colorScheme.error, onStop)
            is VoiceRecordingUiState.Recorded -> MainButton(
                icon = if (state.playing) R.drawable.ic_pause_fill else R.drawable.ic_play_fill,
                description = if (state.playing) R.string.voice_pause else R.string.voice_play,
                color = MaterialTheme.colorScheme.primary,
                onClick = onTogglePlayback,
            )
            else -> MainButton(R.drawable.ic_microphone, R.string.voice_record_again, MaterialTheme.colorScheme.primary, onRetry)
        }
        IconButton(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.voice_save),
                tint = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** The design's 72 dp round button, filled with [color]. */
@Composable
private fun MainButton(@DrawableRes icon: Int, @StringRes description: Int, color: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .background(color, CircleShape),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@StringRes
private fun VoiceFailure.message(): Int = when (this) {
    VoiceFailure.MICROPHONE_UNAVAILABLE -> R.string.voice_microphone_unavailable
    VoiceFailure.TOO_SHORT -> R.string.voice_too_short
    VoiceFailure.PLAYBACK -> R.string.voice_playback_failed
}

/** "0:12", "12:05": minutes and seconds, like the design's counter. */
internal fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Why the microphone is needed, and the button to allow it (or to open the settings). */
@Composable
private fun MicrophonePermissionContent(status: PermissionStatus, onRequest: () -> Unit) {
    val context = LocalContext.current
    val permanentlyDenied = status == PermissionStatus.PERMANENTLY_DENIED
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_microphone),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.voice_permission_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(if (permanentlyDenied) R.string.voice_permission_denied_message else R.string.voice_permission_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(if (permanentlyDenied) R.string.camera_permission_open_settings else R.string.voice_permission_allow),
            onClick = {
                if (permanentlyDenied) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                } else {
                    onRequest()
                }
            },
            icon = if (permanentlyDenied) null else R.drawable.ic_microphone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
