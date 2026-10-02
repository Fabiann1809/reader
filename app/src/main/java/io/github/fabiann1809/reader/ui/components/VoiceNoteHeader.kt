package io.github.fabiann1809.reader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.NoteTag
import io.github.fabiann1809.reader.ui.voice.label

/**
 * A voice note's header in a list (T13.5): the microphone, "Nota de voz" with its [tag], and the
 * button that plays or pauses its recording.
 */
@Composable
fun VoiceNoteHeader(
    tag: NoteTag?,
    isPlaying: Boolean,
    playbackFailed: Boolean,
    onTogglePlayback: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_microphone),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
        ) {
            val label = stringResource(R.string.note_type_voice)
            Text(
                text = if (tag != null) "$label · ${stringResource(tag.label())}" else label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (playbackFailed) {
                Text(
                    text = stringResource(R.string.voice_playback_failed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        FilledTonalIconButton(onClick = onTogglePlayback) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.ic_pause_fill else R.drawable.ic_play_fill),
                contentDescription = stringResource(if (isPlaying) R.string.voice_pause else R.string.voice_play),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
