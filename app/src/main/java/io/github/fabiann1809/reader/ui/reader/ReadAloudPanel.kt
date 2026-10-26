package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloudState
import io.github.fabiann1809.reader.data.reader.readaloud.ReadAloudStatus
import java.text.DecimalFormat

/** About the height of the reader's bottom bar: the panel stays above it while the controls show. */
val ControlsBarClearance = 170.dp

/**
 * The card that shows while the book is read aloud (T11.15): previous and next sentence, play and
 * pause, the speed (which opens the voice settings) and a button to stop. While the voice engine
 * starts, or when it can't, it says so instead.
 */
@Composable
fun ReadAloudPanel(
    state: ReadAloudState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.status == ReadAloudStatus.OFF) return
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
    ) {
        when (state.status) {
            ReadAloudStatus.STARTING -> Message(R.string.read_aloud_preparing, onStop, loading = true)
            ReadAloudStatus.FAILED -> Message(R.string.read_aloud_failed, onStop)
            else -> Controls(state, onPlayPause, onPrevious, onNext, onSettings, onStop)
        }
    }
}

@Composable
private fun Controls(
    state: ReadAloudState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit,
    onStop: () -> Unit,
) {
    val playing = state.status == ReadAloudStatus.PLAYING
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_caret_left), contentDescription = stringResource(R.string.read_aloud_previous))
        }
        FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(56.dp), shape = CircleShape) {
            Icon(
                painterResource(if (playing) R.drawable.ic_pause_fill else R.drawable.ic_play_fill),
                contentDescription = stringResource(if (playing) R.string.read_aloud_pause else R.string.read_aloud_play),
            )
        }
        IconButton(onClick = onNext) {
            Icon(painterResource(R.drawable.ic_caret_right), contentDescription = stringResource(R.string.read_aloud_next))
        }
        // Takes the free space, so the speed and the stop button sit at the end.
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
            TextButton(onClick = onSettings) {
                Text(speedLabel(state.speed), style = MaterialTheme.typography.titleMedium)
            }
        }
        IconButton(onClick = onStop) {
            Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.read_aloud_stop))
        }
    }
}

@Composable
private fun Message(message: Int, onStop: () -> Unit, loading: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onStop) {
            Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.read_aloud_stop))
        }
    }
}

/** "1×", "0,75×": the speed as the user reads it. */
fun speedLabel(speed: Double): String = DecimalFormat("0.##").format(speed) + "×"
