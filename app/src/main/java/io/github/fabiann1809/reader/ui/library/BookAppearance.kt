package io.github.fabiann1809.reader.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion

// Design 8: books rise 8 dp and fade in, staggered 30 ms. Books further down share the last delay,
// so a long library does not keep the ones scrolled into view hidden.
private val RISE = 8.dp
private const val STAGGER_MILLIS = 30
private const val MAX_STAGGERED_BOOKS = 12

/**
 * How far book [index] has appeared (0 = hidden, 1 = in place) [elapsedMillis] after the shelves load.
 * With [reduceMotion] every book fades in together, with no delay and no rise.
 */
fun bookAppearanceProgress(elapsedMillis: Float, index: Int, reduceMotion: Boolean): Float {
    if (reduceMotion) return (elapsedMillis / Motion.INSTANT_MILLIS).coerceIn(0f, 1f)
    val delay = minOf(index, MAX_STAGGERED_BOOKS) * STAGGER_MILLIS
    val linear = ((elapsedMillis - delay) / Motion.MEDIUM_MILLIS).coerceIn(0f, 1f)
    return Motion.MediumEasing.transform(linear)
}

/** One clock for every book, so books scrolled into view later are already in place. */
@Stable
class BookAppearance(private val reduceMotion: Boolean, isFinished: Boolean) {
    private val totalMillis =
        if (reduceMotion) Motion.INSTANT_MILLIS else Motion.MEDIUM_MILLIS + MAX_STAGGERED_BOOKS * STAGGER_MILLIS
    private val elapsed = Animatable(if (isFinished) totalMillis.toFloat() else 0f)

    suspend fun play() {
        elapsed.animateTo(totalMillis.toFloat(), tween(totalMillis, easing = LinearEasing))
    }

    fun progress(index: Int): Float = bookAppearanceProgress(elapsed.value, index, reduceMotion)

    val rises: Boolean = !reduceMotion
}

// The progress is read inside graphicsLayer, so the animation redraws the books without recomposing them.
fun Modifier.bookAppearance(appearance: BookAppearance, index: Int): Modifier = graphicsLayer {
    val progress = appearance.progress(index)
    alpha = progress
    if (appearance.rises) translationY = (1f - progress) * RISE.toPx()
}

/**
 * Plays the appearance once, the first time the library has books to show. Returning from a book
 * or another tab restores the saved flag, so it does not replay.
 */
@Composable
fun rememberBookAppearance(hasBooks: Boolean): BookAppearance {
    val reduceMotion = rememberReduceMotion()
    var hasPlayed by rememberSaveable { mutableStateOf(false) }
    val appearance = remember { BookAppearance(reduceMotion, isFinished = hasPlayed) }
    LaunchedEffect(hasBooks) {
        if (hasBooks && !hasPlayed) {
            hasPlayed = true
            appearance.play()
        }
    }
    return appearance
}
