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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.BookFormat
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import kotlin.math.roundToInt

// The bars are the page's own color at 97 % (the app's surface for PDFs), with a thin line on the page side.
private const val BAR_ALPHA = 0.97f
private const val LINE_ALPHA = 0.12f
private const val SECONDARY_ALPHA = 0.6f

private class BarColors(val bar: Color, val content: Color, val line: Color)

@Composable
private fun barColors(state: ReaderUiState.Ready): BarColors {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val page = state.readingSettings.pageColors()
    val (background, text) = if (state.format == BookFormat.EPUB) Color(page.background) to Color(page.text) else surface to onSurface
    return BarColors(bar = background.copy(alpha = BAR_ALPHA), content = text, line = text.copy(alpha = LINE_ALPHA))
}

/**
 * The reader's overlay: a top bar (back, chapter and book, bookmark, menu) and a bottom bar (the
 * progress bar with positions, and the reading actions). They slide in and fade in 180 ms.
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
    onAi: () -> Unit = {},
    onNotes: () -> Unit = {},
    onRecord: () -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    val colors = barColors(state)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = barEnter(fromTop = true, reduceMotion),
            exit = barExit(toTop = true, reduceMotion),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopBar(state, colors, onBack = onBack, onBookmark = onBookmark, onMenu = onMenu)
        }
        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = barEnter(fromTop = false, reduceMotion),
            exit = barExit(toTop = false, reduceMotion),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomBar(state, colors, onSeek) {
                ControlAction(R.drawable.ic_list_bullets, R.string.reader_index, R.string.reader_index, onIndex)
                ControlAction(R.drawable.ic_text_aa, R.string.reader_text_settings, R.string.reader_text_settings_description, onTextSettings)
                AiAction(onAi)
                ControlAction(R.drawable.ic_note_pencil, R.string.reader_notes, R.string.reader_notes_description, onNotes)
                ControlAction(R.drawable.ic_microphone, R.string.reader_record, R.string.reader_record_description, onRecord)
            }
        }
    }
}

// Slide from the edge plus fade; with Reduce Motion, only a quick fade.
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
private fun ControlsBar(colors: BarColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Column(Modifier.fillMaxWidth().background(colors.bar)) { content() }
    }
}

@Composable
private fun TopBar(state: ReaderUiState.Ready, colors: BarColors, onBack: () -> Unit, onBookmark: () -> Unit, onMenu: () -> Unit) {
    ControlsBar(colors) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            BarIcon(R.drawable.ic_arrow_left, R.string.navigate_up, onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = state.chapter ?: state.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.chapter != null) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(SECONDARY_ALPHA),
                    )
                }
            }
            if (state.pageIsBookmarked) {
                BarIcon(R.drawable.ic_bookmark_simple_fill, R.string.reader_bookmark_remove, onBookmark, tint = MaterialTheme.colorScheme.primary)
            } else {
                BarIcon(R.drawable.ic_bookmark_simple, R.string.reader_bookmark_add, onBookmark)
            }
            BarIcon(R.drawable.ic_dots_three_vertical, R.string.more_options, onMenu)
        }
        HorizontalDivider(color = colors.line)
    }
}

@Composable
private fun BarIcon(@DrawableRes icon: Int, description: Int, onClick: () -> Unit, tint: Color = LocalContentColor.current) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(description), tint = tint)
    }
}

@Composable
private fun BottomBar(state: ReaderUiState.Ready, colors: BarColors, onSeek: (Float) -> Unit, actions: @Composable () -> Unit) {
    ControlsBar(colors) {
        HorizontalDivider(color = colors.line)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
        ) {
            ProgressRow(state.positionCount, state.progression, colors, onSeek)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { actions() }
        }
    }
}

/** The draggable progress bar with where the reader is and where the book ends, in positions. */
@Composable
private fun ProgressRow(positionCount: Int?, progression: Float?, colors: BarColors, onSeek: (Float) -> Unit) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val value = dragged ?: progression ?: 0f
    val progressDescription = stringResource(R.string.reader_progress)
    val here = positionCount?.let { (value * it).roundToInt().coerceAtLeast(1).toString() }
        ?: stringResource(R.string.reader_progress_percent, (value * 100).roundToInt())
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 6.dp),
    ) {
        Text(here, style = MaterialTheme.typography.labelMedium, color = colors.content.copy(alpha = SECONDARY_ALPHA), maxLines = 1)
        ReaderSlider(
            value = value,
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let(onSeek)
                dragged = null
            },
            enabled = progression != null,
            inactiveColor = colors.line,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = progressDescription },
        )
        positionCount?.let {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.content.copy(alpha = SECONDARY_ALPHA),
                maxLines = 1,
                modifier = Modifier.widthIn(min = 24.dp),
            )
        }
    }
}

/** An icon with its short label below; screen readers hear [description] instead of the label (e.g. "Aa"). */
@Composable
private fun ControlAction(
    @DrawableRes icon: Int,
    label: Int,
    description: Int,
    onClick: () -> Unit,
) {
    val descriptionText = stringResource(description)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = Modifier
            .size(width = 60.dp, height = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descriptionText },
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            modifier = Modifier
                .alpha(0.7f)
                .clearAndSetSemantics {},
        )
    }
}

/** "IA" stands out as a filled lavender tile, as every AI feature does. */
@Composable
private fun AiAction(onClick: () -> Unit) {
    val descriptionText = stringResource(R.string.reader_ai_description)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        modifier = Modifier
            .sizeIn(minWidth = 64.dp, minHeight = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descriptionText },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(R.string.reader_ai),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
