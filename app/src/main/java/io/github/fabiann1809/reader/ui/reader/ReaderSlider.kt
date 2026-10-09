package io.github.fabiann1809.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/**
 * The reader's slider (design 7.9): 4 dp track filled with the progress color and a 20 dp round
 * thumb, instead of Material's thick track, bar thumb and step dots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    val colors = SliderDefaults.colors(
        activeTrackColor = ReaderTheme.colors.progress,
        inactiveTrackColor = inactiveColor,
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        colors = colors,
        thumb = {
            // A soft ring around the 20 dp dot, so it is easy to grab.
            Box(
                Modifier
                    .size(20.dp)
                    .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                enabled = enabled,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(4.dp),
            )
        },
        modifier = modifier,
    )
}
