package io.github.fabiann1809.reader.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R

// Pieces shared by the onboarding, the welcome and the sign-in (design "Reader Acceso").

private val SparkColor = Color(0xFFF2B544)

/** The 48 dp round button at the top left of those screens. */
@Composable
fun RoundBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_caret_left),
                contentDescription = stringResource(R.string.navigate_up),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** A star that breathes (design `pulse`); still with Reduce Motion. */
@Composable
fun PulsingSparkle(x: Int, y: Int, size: Int, delayMillis: Int = 0) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "sparkle")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1_100, delayMillis = delayMillis, easing = Motion.MediumEasing),
            RepeatMode.Reverse,
        ),
        label = "sparkleScale",
    )
    val amount = if (reduceMotion) 0f else pulse
    Icon(
        painter = painterResource(R.drawable.ic_sparkle),
        contentDescription = null,
        tint = SparkColor,
        modifier = Modifier
            .offset(x.dp, y.dp)
            .size(size.dp)
            .graphicsLayer {
                val scale = 1f + 0.14f * amount
                scaleX = scale
                scaleY = scale
                alpha = 0.75f + 0.25f * amount
            },
    )
}

/** Entrance `fadeUp` of the design: rises 16 dp while it appears. */
fun Modifier.fadeUp(delayMillis: Int = 0): Modifier = composed {
    val progress = rememberEntranceProgress(delayMillis, durationMillis = 450)
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 16.dp.toPx()
    }
}

/** Entrance `shelfIn`: drops from above with a slight tilt and overshoots into place. */
fun Modifier.dropIn(delayMillis: Int = 0): Modifier = composed {
    val progress = rememberEntranceProgress(delayMillis, durationMillis = 550, easing = Motion.PopEasing)
    graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * -26.dp.toPx()
        rotationZ = (1f - progress) * -4f
    }
}

/** Entrance `popIn`: scales up from 88% while it appears. */
fun Modifier.popIn(delayMillis: Int = 0): Modifier = composed {
    val progress = rememberEntranceProgress(delayMillis, durationMillis = 500, easing = Motion.PopEasing)
    graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        val scale = 0.88f + 0.12f * progress
        scaleX = scale
        scaleY = scale
    }
}
