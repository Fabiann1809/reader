package io.github.fabiann1809.reader.ui.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloudState
import io.github.fabiann1809.reader.data.reader.readaloud.SpeechVoice
import io.github.fabiann1809.reader.data.reader.readaloud.VoiceLevel
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip

private val SPEEDS = listOf(0.75, 1.0, 1.25, 1.5, 2.0)
private val SLEEP_MINUTES = listOf(15, 30, 60)

/** The voice settings of the read aloud (T11.15): speed, which voice speaks, and a sleep timer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadAloudSheet(
    state: ReadAloudState,
    onSpeed: (Double) -> Unit,
    onVoice: (String) -> Unit,
    onSleepTimer: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(stringResource(R.string.read_aloud_title), style = MaterialTheme.typography.headlineSmall)
            Section(R.string.read_aloud_speed) {
                Chips {
                    SPEEDS.forEach { speed ->
                        ReaderFilterChip(selected = speed == state.speed, onClick = { onSpeed(speed) }, label = { Text(speedLabel(speed)) })
                    }
                }
            }
            Section(R.string.read_aloud_voice) { Voices(state, onVoice) }
            Section(R.string.read_aloud_timer) {
                Chips {
                    ReaderFilterChip(
                        selected = state.sleepMinutes == null,
                        onClick = { onSleepTimer(null) },
                        label = { Text(stringResource(R.string.read_aloud_timer_off)) },
                    )
                    SLEEP_MINUTES.forEach { minutes ->
                        ReaderFilterChip(
                            selected = state.sleepMinutes == minutes,
                            onClick = { onSleepTimer(minutes) },
                            label = { Text(stringResource(R.string.read_aloud_timer_minutes, minutes)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun Chips(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) { content() }
}

@Composable
private fun Voices(state: ReadAloudState, onVoice: (String) -> Unit) {
    if (state.voices.size < 2) {
        Text(stringResource(R.string.read_aloud_no_voices), style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column {
        state.voices.forEachIndexed { index, voice ->
            VoiceRow(number = index + 1, voice = voice, selected = voice.id == state.voiceId, onClick = { onVoice(voice.id) })
        }
    }
}

@Composable
private fun VoiceRow(number: Int, voice: SpeechVoice, selected: Boolean, onClick: () -> Unit) {
    val level = stringResource(voice.level.label())
    val detail = if (voice.online) "$level · ${stringResource(R.string.read_aloud_online)}" else level
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.read_aloud_voice_name, number), style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@StringRes
private fun VoiceLevel.label(): Int = when (this) {
    VoiceLevel.BASIC -> R.string.read_aloud_level_basic
    VoiceLevel.GOOD -> R.string.read_aloud_level_good
    VoiceLevel.NATURAL -> R.string.read_aloud_level_natural
}
