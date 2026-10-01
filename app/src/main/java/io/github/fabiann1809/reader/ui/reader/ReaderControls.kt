package io.github.fabiann1809.reader.ui.reader

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.roundToInt

// Surface at 94 % with level 3 shadow (design 7.8).
private const val BAR_ALPHA = 0.94f
private val BarElevation = 6.dp

/**
 * The reader's overlay (design 7.8): a top bar (back, title, bookmark, menu) and a bottom bar
 * (progress bar with the chapter, and the reading actions). They slide in and fade in 180 ms.
 * Actions without a feature yet get one in their own task: bookmarks and index (T11.6), "Aa"
 * (T11.7), voice (T11.15), AI (T11.11) and recording (phase 13).
 */
@Composable
fun ReaderControls(
    state: ReaderUiState.Ready,
    onBack: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onBookmark: () -> Unit = {},
    onMenu: () -> Unit = {},
    onIndex: () -> Unit = {},
    onTextSettings: () -> Unit = {},
    onVoice: () -> Unit = {},
    onAi: () -> Unit = {},
    onRecord: () -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = barEnter(fromTop = true, reduceMotion),
            exit = barExit(toTop = true, reduceMotion),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopBar(title = state.title, onBack = onBack, onBookmark = onBookmark, onMenu = onMenu)
        }
        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = barEnter(fromTop = false, reduceMotion),
            exit = barExit(toTop = false, reduceMotion),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomBar(state, onSeek) {
                ControlAction(R.drawable.ic_list_bullets, R.string.reader_index, R.string.reader_index, onIndex)
                ControlAction(R.drawable.ic_text_aa, R.string.reader_text_settings, R.string.reader_text_settings_description, onTextSettings)
                ControlAction(R.drawable.ic_speaker_high, R.string.reader_voice, R.string.reader_voice_description, onVoice)
                // The AI stands out with the lavender accent, never with a solid fill (design 02 §3.4).
                ControlAction(R.drawable.ic_sparkle, R.string.reader_ai, R.string.reader_ai_description, onAi, ReaderTheme.colors.ai)
                ControlAction(R.drawable.ic_microphone, R.string.reader_record, R.string.reader_record_description, onRecord)
            }
        }
    }
}

// Slide from the edge plus fade; with Reduce Motion, only a quick fade (design 03 §5).
private fun barEnter(fromTop: Boolean, reduceMotion: Boolean): EnterTransition {
    if (reduceMotion) return fadeIn(tween(Motion.INSTANT_MILLIS))
    return fadeIn(shortTween()) + slideInVertically(shortTween()) { if (fromTop) -it else it }
}

private fun barExit(toTop: Boolean, reduceMotion: Boolean): ExitTransition {
    if (reduceMotion) return fadeOut(tween(Motion.INSTANT_MILLIS))
    return fadeOut(shortTween()) + slideOutVertically(shortTween()) { if (toTop) -it else it }
}

// `motion-short`: 180 ms, ease-out.
private fun <T> shortTween() = tween<T>(Motion.SHORT_MILLIS, easing = LinearOutSlowInEasing)

@Composable
private fun ControlsBar(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = BAR_ALPHA),
        shadowElevation = BarElevation,
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit, onBookmark: () -> Unit, onMenu: () -> Unit) {
    ControlsBar {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            BarIcon(R.drawable.ic_caret_left, R.string.navigate_up, onBack)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            BarIcon(R.drawable.ic_bookmark_simple, R.string.reader_bookmark, onBookmark)
            BarIcon(R.drawable.ic_dots_three_vertical, R.string.more_options, onMenu)
        }
    }
}

@Composable
private fun BarIcon(@DrawableRes icon: Int, description: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(description))
    }
}

@Composable
private fun BottomBar(state: ReaderUiState.Ready, onSeek: (Float) -> Unit, actions: @Composable () -> Unit) {
    ControlsBar {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            ProgressRow(state.chapter, state.progression, onSeek)
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) { actions() }
        }
    }
}

/** The draggable progress bar (design 7.9); it shows the chapter, or the percentage while dragging. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressRow(chapter: String?, progression: Float?, onSeek: (Float) -> Unit) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val value = dragged ?: progression ?: 0f
    val percent = stringResource(R.string.reader_progress_percent, (value * 100).roundToInt())
    val progressDescription = stringResource(R.string.reader_progress)
    val enabled = progression != null
    val colors = SliderDefaults.colors(
        activeTrackColor = ReaderTheme.colors.progress,
        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slider(
            value = value,
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let(onSeek)
                dragged = null
            },
            enabled = enabled,
            colors = colors,
            // Thin track and round thumb from the design, instead of Material's thick track and bar thumb.
            thumb = {
                Box(
                    Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = colors,
                    enabled = enabled,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    modifier = Modifier.height(4.dp),
                )
            },
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = progressDescription },
        )
        Text(
            text = if (dragged == null && chapter != null) chapter else percent,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
    }
}

/** An icon with its short label below; screen readers hear [description] instead of the label (e.g. "Aa"). */
@Composable
private fun ControlAction(
    @DrawableRes icon: Int,
    label: Int,
    description: Int,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val descriptionText = stringResource(description)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descriptionText }
            .padding(vertical = 6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
