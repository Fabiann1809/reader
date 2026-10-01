package io.github.fabiann1809.reader.ui.components

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
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
}

/**
 * True when the user turned animations off ("Quitar animaciones" in the accessibility settings),
 * which Android exposes as an animator duration scale of 0.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
