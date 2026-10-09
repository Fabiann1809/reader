package io.github.fabiann1809.reader.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/**
 * The streak's flame (T16.5). When [celebrate] (today's goal just met), it flares up once (grows
 * with a little wobble and settles) and then reports [onCelebrated]; with reduced motion it doesn't
 * move, but the celebration still counts as done.
 */
@Composable
fun StreakFlame(celebrate: Boolean, onCelebrated: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val wobble = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion()
    val done by rememberUpdatedState(onCelebrated)
    LaunchedEffect(celebrate) {
        if (!celebrate) return@LaunchedEffect
        if (!reduceMotion) {
            scale.animateTo(FLARE_SCALE, tween(FLARE_MILLIS))
            wobble.animateTo(WOBBLE_DEGREES, tween(WOBBLE_MILLIS))
            wobble.animateTo(-WOBBLE_DEGREES, tween(WOBBLE_MILLIS * 2))
            wobble.animateTo(0f, tween(WOBBLE_MILLIS))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        done()
    }
    Icon(
        painterResource(R.drawable.ic_flame_fill),
        contentDescription = null,
        tint = ReaderTheme.colors.warning,
        modifier = Modifier
            .testTag(STREAK_FLAME_TAG)
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = wobble.value
            },
    )
}

const val STREAK_FLAME_TAG = "streak_flame"

private const val FLARE_SCALE = 1.8f
private const val FLARE_MILLIS = 250
private const val WOBBLE_DEGREES = 12f
private const val WOBBLE_MILLIS = 90
