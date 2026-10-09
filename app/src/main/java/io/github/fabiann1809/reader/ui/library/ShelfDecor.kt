package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.rememberEntranceProgress
import kotlin.math.cos
import kotlin.math.sin

/** Small objects that keep a shelf from looking bare, drawn in code like the rest of the wall. */
enum class ShelfDecor(val width: Dp, val height: Dp) {
    BOOKEND(58.dp, 120.dp),
    VASE(62.dp, 132.dp),
    STACK(74.dp, 90.dp),
}

/** Width reserved on a shelf for its decoration: the widest one. */
val ShelfDecorSlot = 64.dp

private const val DECOR_ENTRANCE_MILLIS = 500

@Composable
fun ShelfDecorItem(decor: ShelfDecor, delayMillis: Int, modifier: Modifier = Modifier) {
    val progress = rememberEntranceProgress(delayMillis, DECOR_ENTRANCE_MILLIS)
    Box(
        modifier
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 16.dp.toPx()
            }
            .size(decor.width, decor.height),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (decor) {
                ShelfDecor.BOOKEND -> drawBookend()
                ShelfDecor.VASE -> drawVase()
                ShelfDecor.STACK -> drawStack()
            }
        }
    }
}

private fun DrawScope.dp(value: Float) = value.dp.toPx()

private fun DrawScope.drawBookend() {
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFF141110),
            0.6f to Color(0xFF3D342E),
            1f to Color(0xFF1E1917),
            startX = dp(10f),
            endX = dp(23f),
        ),
        topLeft = Offset(dp(10f), dp(8f)),
        size = Size(dp(13f), dp(112f)),
        cornerRadius = CornerRadius(dp(3f)),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF3D342E), Color(0xFF141110)), startY = dp(109f), endY = dp(120f)),
        topLeft = Offset(dp(10f), dp(109f)),
        size = Size(dp(46f), dp(11f)),
        cornerRadius = CornerRadius(dp(3f)),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFC9A26B),
            0.7f to Color(0xFF7A5A2E),
            center = Offset(dp(30f), dp(95f)),
            radius = dp(14f),
        ),
        radius = dp(10f),
        center = Offset(dp(33f), dp(99f)),
    )
}

private fun DrawScope.drawVase() {
    val stem = Color(0xFF8B7355)
    val stroke = Stroke(width = dp(2f), cap = StrokeCap.Butt)
    drawStem(Offset(dp(31f), dp(80f)), lengthDp = 74f, degrees = -14f, color = stem, stroke = stroke)
    drawStem(Offset(dp(32f), dp(80f)), lengthDp = 66f, degrees = 10f, color = stem, stroke = stroke)
    rotate(-20f, pivot = Offset(dp(21f), dp(13f))) {
        drawOval(Color(0xFFD9B26F), Offset(dp(14f), dp(2f)), Size(dp(14f), dp(22f)))
    }
    rotate(18f, pivot = Offset(dp(42f), dp(22f))) {
        drawOval(Color(0xFFE0A458), Offset(dp(36f), dp(12f)), Size(dp(12f), dp(20f)))
    }
    drawRoundRect(Color(0xFFC9876A), Offset(dp(23f), dp(62f)), Size(dp(18f), dp(12f)), CornerRadius(dp(4f)))
    val body = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(Offset(dp(8f), dp(70f)), Size(dp(48f), dp(62f))),
                topLeft = CornerRadius(dp(21f), dp(27f)),
                topRight = CornerRadius(dp(21f), dp(27f)),
                bottomLeft = CornerRadius(dp(14f)),
                bottomRight = CornerRadius(dp(14f)),
            ),
        )
    }
    drawPath(
        body,
        Brush.radialGradient(
            0f to Color(0xFFE7A486),
            0.75f to Color(0xFFB9674A),
            center = Offset(dp(24f), dp(89f)),
            radius = dp(36f),
        ),
    )
}

private fun DrawScope.drawStem(start: Offset, lengthDp: Float, degrees: Float, color: Color, stroke: Stroke) {
    val radians = Math.toRadians(degrees.toDouble())
    val end = Offset(start.x + dp(lengthDp) * sin(radians).toFloat(), start.y - dp(lengthDp) * cos(radians).toFloat())
    drawLine(color, start, end, strokeWidth = stroke.width)
}

private fun DrawScope.drawStack() {
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF9CC5B4),
            0.75f to Color(0xFF3E6B5C),
            center = Offset(dp(33f), dp(15f)),
            radius = dp(20f),
        ),
        radius = dp(14f),
        center = Offset(dp(38f), dp(20f)),
    )
    drawStackedBook(top = 36f, left = 4f, width = 64f, cover = Color(0xFFE8D9BF), spine = Color(0xFFB4652B))
    drawStackedBook(top = 54f, left = 0f, width = 72f, cover = Color(0xFF2F4858), spine = Color(0xFF1D2E39))
    drawStackedBook(top = 72f, left = 3f, width = 68f, cover = Color(0xFF8C3B3B), spine = Color(0xFF5E2626))
}

private fun DrawScope.drawStackedBook(top: Float, left: Float, width: Float, cover: Color, spine: Color) {
    val corner = CornerRadius(dp(2f))
    drawRoundRect(cover, Offset(dp(left), dp(top)), Size(dp(width), dp(18f)), corner)
    drawRoundRect(spine, Offset(dp(left), dp(top)), Size(dp(5f), dp(18f)), corner)
}
