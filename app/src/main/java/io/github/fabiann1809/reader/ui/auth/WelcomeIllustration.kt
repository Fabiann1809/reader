package io.github.fabiann1809.reader.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private val Cover = Color(0xFF5E3418)
private val CoverOrange = Color(0xFFD9673A)
private val Paper = Color(0xFFFBF3E6)
private val PaperLine = Color(0xFFE8D9C2)
private val Spark = Color(0xFFF2B544)
private val BookShape = RoundedCornerShape(topStart = 6.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 6.dp)

/** Two books lying down, an open one on top, loose pages and sparks between two leaves (300 x 206 dp). */
@Composable
fun WelcomeIllustration(modifier: Modifier = Modifier) {
    Box(modifier.size(300.dp, 206.dp)) {
        Leaf(x = -6, y = 54, size = 92, rotation = -30f, mirrored = false)
        Leaf(x = 214, y = 60, size = 84, rotation = -28f, mirrored = true)
        Box(Modifier.offset(30.dp, 156.dp).size(240.dp, 44.dp).clip(CircleShape).background(ReaderTheme.colors.authWave))
        LyingBook(x = 46, y = 140, width = 208, height = 46, color = Cover, rotation = 0f, pagesFrom = 96, spines = listOf(18, 28))
        LyingBook(x = 58, y = 102, width = 184, height = 40, color = CoverOrange, rotation = -3f, pagesFrom = 86, spines = listOf(16, 26))
        OpenBook(Modifier.offset(78.dp, 56.dp).size(144.dp, 48.dp))
        LoosePage(x = 86, y = 8, width = 40, height = 50, rotation = -18f)
        LoosePage(x = 170, y = 20, width = 44, height = 52, rotation = 16f)
        Sparkle(x = 44, y = 30, size = 26)
        Sparkle(x = 222, y = 6, size = 32)
        Sparkle(x = 140, y = 56, size = 16)
    }
}

@Composable
private fun Leaf(x: Int, y: Int, size: Int, rotation: Float, mirrored: Boolean) {
    Icon(
        painter = painterResource(R.drawable.ic_leaf_fill),
        contentDescription = null,
        tint = ReaderTheme.colors.authLeaf,
        modifier = Modifier
            .offset(x.dp, y.dp)
            .size(size.dp)
            .graphicsLayer { scaleX = if (mirrored) -1f else 1f }
            .rotate(rotation),
    )
}

@Composable
private fun LyingBook(x: Int, y: Int, width: Int, height: Int, color: Color, rotation: Float, pagesFrom: Int, spines: List<Int>) {
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(width.dp, height.dp)
            .rotate(rotation)
            .shadow(8.dp, BookShape)
            .clip(BookShape)
            .background(color),
    ) {
        Pages(
            Modifier
                .offset(pagesFrom.dp, 6.dp)
                .size((width - pagesFrom).dp, (height - 12).dp)
                .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)),
        )
        spines.forEach { start ->
            Box(Modifier.offset(start.dp, 8.dp).size(3.dp, (height - 16).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
        }
    }
}

@Composable
private fun OpenBook(modifier: Modifier) {
    Box(modifier) {
        Box(Modifier.offset(0.dp, 36.dp).size(144.dp, 12.dp).clip(RoundedCornerShape(4.dp)).background(Cover))
        Box(
            Modifier
                .offset(6.dp, 6.dp)
                .size(66.dp, 34.dp)
                .rotate(6f)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 4.dp))
                .background(Paper),
        )
        Box(
            Modifier
                .offset(72.dp, 6.dp)
                .size(66.dp, 34.dp)
                .rotate(-6f)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp))
                .background(Paper),
        )
    }
}

@Composable
private fun LoosePage(x: Int, y: Int, width: Int, height: Int, rotation: Float) {
    Pages(
        Modifier
            .offset(x.dp, y.dp)
            .size(width.dp, height.dp)
            .rotate(rotation)
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp)),
        lineEvery = 7.dp,
    )
}

@Composable
private fun Sparkle(x: Int, y: Int, size: Int) {
    Icon(
        painter = painterResource(R.drawable.ic_sparkle),
        contentDescription = null,
        tint = Spark,
        modifier = Modifier.offset(x.dp, y.dp).size(size.dp),
    )
}

/** Paper with a thin line every few dp, like the edge of a block of pages. */
@Composable
private fun Pages(modifier: Modifier, lineEvery: Dp = 4.dp) {
    Canvas(modifier.background(Paper)) {
        val step = lineEvery.toPx()
        var y = step - 1.dp.toPx()
        while (y < size.height) {
            drawLine(PaperLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            y += step
        }
    }
}
