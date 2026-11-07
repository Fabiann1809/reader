package io.github.fabiann1809.reader.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.DmSerifDisplay
import kotlin.math.cos
import kotlin.math.sin

private val SparkColor = Color(0xFFE07A3F)

/**
 * The mark of the sign-in design: an open book with three rays, and "Reader" in the serif of the
 * covers. [iconSize] is the book (76 dp on the welcome, 60 dp on the form); the rest scales with it.
 */
@Composable
fun ReaderLogo(iconSize: Dp, modifier: Modifier = Modifier) {
    val scale = iconSize.value / 76f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Rays(scale)
        Icon(
            painter = painterResource(R.drawable.ic_book_open_bold),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize).offset(y = (-14 * scale).dp),
        )
        Text(
            text = stringResource(R.string.app_name),
            fontFamily = DmSerifDisplay,
            fontSize = (60 * scale).sp,
            lineHeight = (60 * scale).sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.offset(y = (-14 * scale).dp),
        )
    }
}

@Composable
private fun Rays(scale: Float) {
    Canvas(Modifier.size((76 * scale).dp, (20 * scale).dp)) {
        val s = scale * density
        val cx = size.width / 2
        val stroke = 4 * s
        drawLine(SparkColor, Offset(cx, 2 * s), Offset(cx, 10 * s), stroke, StrokeCap.Round)
        for (side in listOf(-1f, 1f)) {
            val angle = Math.toRadians(40.0)
            val dx = (side * sin(angle) * 3 * s).toFloat()
            val dy = (cos(angle) * 3 * s).toFloat()
            val center = Offset(cx + side * 15 * s, 10 * s)
            drawLine(SparkColor, Offset(center.x + dx, center.y - dy), Offset(center.x - dx, center.y + dy), stroke, StrokeCap.Round)
        }
    }
}
