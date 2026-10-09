package io.github.fabiann1809.reader.ui.voice

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.note.NoteTag

private const val WAVE_BARS = 24

// A silent microphone still shows a thin line, as in the design.
private const val MIN_BAR_FRACTION = 0.08f

/** The recording's wave (lámina 1i): one bar per recent microphone level, newest on the right. */
@Composable
fun VoiceWave(levels: List<Float>, modifier: Modifier = Modifier) {
    // Padded on the left with silence until there are enough levels to fill the wave.
    val bars = List((WAVE_BARS - levels.size).coerceAtLeast(0)) { 0f } + levels.takeLast(WAVE_BARS)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(56.dp),
    ) {
        bars.forEach { level ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(level.coerceIn(MIN_BAR_FRACTION, 1f))
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
            )
        }
    }
}

/** Idea · Duda · Cita · Tarea: at most one is picked; tapping it again clears it. */
@Composable
fun TagChips(selected: NoteTag?, onToggle: (NoteTag) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        NoteTag.entries.forEach { tag ->
            ReaderFilterChip(
                selected = tag == selected,
                onClick = { onToggle(tag) },
                label = { Text(stringResource(tag.label())) },
            )
        }
    }
}

@StringRes
fun NoteTag.label(): Int = when (this) {
    NoteTag.IDEA -> R.string.note_tag_idea
    NoteTag.DOUBT -> R.string.note_tag_doubt
    NoteTag.QUOTE -> R.string.note_tag_quote
    NoteTag.TASK -> R.string.note_tag_task
}
