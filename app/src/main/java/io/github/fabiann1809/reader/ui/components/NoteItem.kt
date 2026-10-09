package io.github.fabiann1809.reader.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.Note
import io.github.fabiann1809.reader.data.note.NoteType
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.ui.voice.label
import io.github.fabiann1809.reader.util.formatDate

/** The icon circle and label of each kind of note: written (accent), AI explanation (lavender), voice (green). */
private class NoteKind(@DrawableRes val icon: Int, val label: String, val container: Color, val ink: Color)

@Composable
private fun Note.kind(): NoteKind {
    val colors = ReaderTheme.colors
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        NoteType.MANUAL -> NoteKind(R.drawable.ic_note_pencil, stringResource(R.string.note_type_manual), scheme.primaryContainer, scheme.onPrimaryContainer)
        NoteType.EXPLANATION -> NoteKind(R.drawable.ic_sparkle, stringResource(R.string.note_type_explanation), colors.aiContainer, colors.ai)
        NoteType.VOICE -> {
            val voice = stringResource(R.string.note_type_voice)
            NoteKind(
                R.drawable.ic_microphone,
                if (tag != null) "$voice · ${stringResource(tag.label())}" else voice,
                colors.successContainer,
                colors.onSuccessContainer,
            )
        }
    }
}

/**
 * A note card: a colored circle with its kind's icon, the kind, the text and when it was written.
 * A note written on a passage (T11.13) opens the book there when tapped ([onOpenInBook]) and is
 * edited with its pencil ([onClick]); any other note is edited by tapping it. A voice note
 * (T13.5) plays its recording with [onTogglePlayback].
 */
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenInBook: (() -> Unit)? = null,
    isPlaying: Boolean = false,
    playbackFailed: Boolean = false,
    onTogglePlayback: () -> Unit = {},
) {
    val openInBook = onOpenInBook.takeIf { note.location != null }
    val kind = note.kind()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = openInBook ?: onClick)
            .padding(14.dp),
    ) {
        KindBadge(kind)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = kind.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = kind.ink,
                    modifier = Modifier.weight(1f),
                )
                if (openInBook != null) EditButton(onClick)
            }
            // A voice note where no words were heard has only its recording.
            val untranscribed = note.type == NoteType.VOICE && note.content.isBlank()
            Text(
                text = if (untranscribed) stringResource(R.string.note_voice_untranscribed) else note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (untranscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.type == NoteType.VOICE) VoicePlayer(isPlaying, playbackFailed, onTogglePlayback)
            Text(
                text = listOfNotNull(note.page?.let { stringResource(R.string.note_page, it) }, formatDate(note.createdAt)).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun KindBadge(kind: NoteKind) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(kind.container, CircleShape),
    ) {
        Icon(painterResource(kind.icon), contentDescription = null, tint = kind.ink, modifier = Modifier.size(20.dp))
    }
}

/** The pencil of a note anchored to a passage: tapping the card goes to the book, this edits it. */
@Composable
private fun EditButton(onEdit: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onEdit),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_note_pencil),
            contentDescription = stringResource(R.string.note_edit),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Play or pause button with a static waveform; the bars light up while it plays. */
@Composable
private fun VoicePlayer(isPlaying: Boolean, playbackFailed: Boolean, onToggle: () -> Unit) {
    val colors = ReaderTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(colors.success, CircleShape)
                .clickable(role = Role.Button, onClick = onToggle),
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.ic_pause_fill else R.drawable.ic_play_fill),
                contentDescription = stringResource(if (isPlaying) R.string.voice_pause else R.string.voice_play),
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .alpha(if (isPlaying) 1f else 0.45f),
        ) {
            WAVE_HEIGHTS.forEach { h -> Box(Modifier.width(3.dp).height(h.dp).background(colors.success, RoundedCornerShape(2.dp))) }
        }
    }
    if (playbackFailed) {
        Text(
            text = stringResource(R.string.voice_playback_failed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private val WAVE_HEIGHTS = listOf(8, 14, 20, 12, 18, 24, 10, 16, 22, 12, 8, 18, 14, 10, 20, 12)

private const val PREVIEW_MAX_LINES = 4
