package io.github.fabiann1809.reader.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Motion tokens from the design (section 8).
object Motion {
    /** `motion-instant`: also the plain fade every transition becomes with Reduce Motion. */
    const val INSTANT_MILLIS = 100

    /** `motion-short` (ease-out): e.g. the reader's controls. */
    const val SHORT_MILLIS = 180

    /** `motion-medium`. */
    const val MEDIUM_MILLIS = 300
    val MediumEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Entrances of the redesign (fade up, pop in, sheets, toasts). */
    const val ENTER_MILLIS = 350

    /** Overshoot used by the "pop" of active icons and the book entrance. */
    val PopEasing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)

    /** Delay between consecutive items of a staggered entrance (shelves, lists). */
    const val STAGGER_MILLIS = 30

    /** Duration to animate with: the plain [INSTANT_MILLIS] fade when Reduce Motion is on. */
    fun duration(millis: Int, reduceMotion: Boolean): Int = if (reduceMotion) INSTANT_MILLIS else millis
}

/**
 * True when the user turned animations off ("Quitar animaciones" in the accessibility settings),
 * which Android exposes as an animator duration scale of 0.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember { isReduceMotionOn(context) }
}

/** [rememberReduceMotion] for views outside Compose (e.g. the reader's page turns). */
fun isReduceMotionOn(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * Progress (0..1) of an entrance that starts after [delayMillis] and runs once; it is already 1
 * with Reduce Motion, so callers need no second code path.
 */
@Composable
fun rememberEntranceProgress(
    delayMillis: Int = 0,
    durationMillis: Int = Motion.ENTER_MILLIS,
    easing: Easing = Motion.MediumEasing,
): Float {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) progress.animateTo(1f, tween(durationMillis, delayMillis, easing))
    }
    return progress.value
}
