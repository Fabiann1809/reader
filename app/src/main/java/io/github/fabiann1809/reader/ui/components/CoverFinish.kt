package io.github.fabiann1809.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * What makes a flat picture look like a book: a soft gloss, the fold of the hinge next to the
 * spine, the light on the top edge, the pages' edge on the right and a little shade at the bottom.
 * It goes over any cover, generated or real.
 */
@Composable
fun CoverFinish() {
    Box(Modifier.fillMaxSize()) {
        // Soft diagonal gloss.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(0f to Color.White.copy(alpha = 0.12f), 0.55f to Color.Transparent)),
        )
        // The bottom darkens a little, so the cover has weight.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(36.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)))),
        )
        // Spine: shade, then the hinge, a thin highlight with a dark line beside it.
        Box(
            Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.26f), Color.Transparent))),
        )
        Box(Modifier.fillMaxHeight().width(1.dp).offset(x = 7.dp).background(Color.White.copy(alpha = 0.22f)))
        Box(Modifier.fillMaxHeight().width(1.dp).offset(x = 8.dp).background(Color.Black.copy(alpha = 0.16f)))
        // Light on the top edge.
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.3f)))
        // The pages' edge, on the right.
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(3.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)))),
        )
    }
}
