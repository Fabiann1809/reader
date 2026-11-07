package io.github.fabiann1809.reader.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.PulsingSparkle
import io.github.fabiann1809.reader.ui.components.dropIn
import io.github.fabiann1809.reader.ui.components.fadeUp
import io.github.fabiann1809.reader.ui.components.popIn
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

// Illustrations of the three onboarding steps (design "Reader Acceso"), drawn with the app's own
// tokens and plain shapes (no images). Each one is 300 x 290 dp on a peach disc.

private val Cover = Color(0xFF5E3418)
private val Paper = Color(0xFFFBF3E6)
private val PaperLine = Color(0xFFD8C6AC)
private val Amber = Color(0xFFF2B544)
private val Ember = Color(0xFFE07A3F)
private val Shelf = Color(0xFF8A5A36)
private val PhoneInner = Color(0xFF3A2618)
private val BookShape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 4.dp)

private class ShelfBook(val width: Int, val height: Int, val cover: Color, val band: Color)

private val ShelfBooks = listOf(
    ShelfBook(52, 132, Cover, Color(0xFFE0A458)),
    ShelfBook(46, 112, Color(0xFFD9673A), Paper),
    ShelfBook(50, 142, Color(0xFFF3E5D2), Cover),
    ShelfBook(40, 98, Color(0xFF5B4B8A), Color(0xFFE7B7C8)),
)

@Composable
private fun IllustrationFrame(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.size(300.dp, 290.dp)) {
        Box(Modifier.offset(30.dp, 30.dp).size(240.dp).clip(CircleShape).background(ReaderTheme.colors.authField))
        content()
    }
}

/** Four books standing on a shelf between two leaves. */
@Composable
fun ShelfIllustration(modifier: Modifier = Modifier) {
    IllustrationFrame(modifier) {
        Leaf(x = 4, y = 120, size = 84, rotation = -30f, mirrored = false)
        Leaf(x = 222, y = 110, size = 76, rotation = -28f, mirrored = true)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp),
        ) {
            ShelfBooks.forEachIndexed { index, book -> StandingBook(book, delayMillis = 100 * (index + 1)) }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 36.dp, bottom = 48.dp)
                .size(228.dp, 14.dp)
                .shadow(6.dp, RoundedCornerShape(4.dp))
                .background(Shelf, RoundedCornerShape(4.dp)),
        )
        PulsingSparkle(x = 54, y = 46, size = 26)
        PulsingSparkle(x = 228, y = 28, size = 30, delayMillis = 500)
        PulsingSparkle(x = 150, y = 10, size = 16, delayMillis = 1_000)
    }
}

@Composable
private fun StandingBook(book: ShelfBook, delayMillis: Int) {
    Box(
        Modifier
            .size(book.width.dp, book.height.dp)
            .dropIn(delayMillis)
            .shadow(8.dp, BookShape)
            .clip(BookShape)
            .background(book.cover),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(5.dp).background(book.band))
        Box(
            Modifier
                .fillMaxHeight()
                .width(7.dp)
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent), endX = 20f)),
        )
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

/** A phone with a tilted page, the scanning line and the four corners of the frame. */
@Composable
fun ScanIllustration(modifier: Modifier = Modifier) {
    IllustrationFrame(modifier) {
        Box(
            Modifier
                .offset(88.dp, 22.dp)
                .size(124.dp, 236.dp)
                .popIn(100)
                .shadow(16.dp, RoundedCornerShape(26.dp))
                .background(Cover, RoundedCornerShape(26.dp))
                .padding(8.dp),
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp)).background(PhoneInner)) {
                TiltedPage()
                ScanLine()
                Canvas(Modifier.fillMaxSize()) { drawFrameCorners() }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .size(22.dp)
                        .background(Paper, CircleShape),
                )
            }
        }
        TextReadyChip(Modifier.offset(196.dp, 150.dp).fadeUp(450))
        PulsingSparkle(x = 46, y = 60, size = 24)
        PulsingSparkle(x = 232, y = 40, size = 28, delayMillis = 600)
    }
}

/** The paper inside the phone: lines of text, two of them highlighted. */
@Composable
private fun TiltedPage() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, end = 14.dp, top = 34.dp, bottom = 44.dp)
            .rotate(-3f)
            .background(Paper, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        listOf(1f to PaperLine, 1f to PaperLine, 0.8f to PaperLine, 1f to Ember, 0.7f to Ember, 1f to PaperLine, 0.6f to PaperLine)
            .forEach { (fraction, color) ->
                Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(color, CircleShape))
            }
    }
}

@Composable
private fun BoxScope.ScanLine() {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "scan")
    val animated by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanPosition",
    )
    val position = if (reduceMotion) 0.5f else animated
    // The line runs between the frame corners: 28 dp from the top and 38 dp from the bottom.
    Box(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 28.dp, bottom = 38.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .height(2.dp)
                .align(Alignment.TopStart)
                .graphicsLayer { translationY = position * 150.dp.toPx() }
                .shadow(12.dp, CircleShape, ambientColor = Amber, spotColor = Amber)
                .background(Amber),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrameCorners() {
    val arm = 20.dp.toPx()
    val radius = 8.dp.toPx()
    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Butt)
    val left = 8.dp.toPx()
    val right = size.width - 8.dp.toPx()
    val top = 24.dp.toPx()
    val bottom = size.height - 34.dp.toPx()
    // Each corner is two arms meeting at a rounded elbow.
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(Amber, Offset(x, y + dy * arm), Offset(x, y + dy * radius), stroke.width)
        drawLine(Amber, Offset(x + dx * arm, y), Offset(x + dx * radius, y), stroke.width)
        drawArc(
            color = Amber,
            startAngle = when {
                dx > 0 && dy > 0 -> 180f
                dx < 0 && dy > 0 -> 270f
                dx < 0 && dy < 0 -> 0f
                else -> 90f
            },
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(if (dx > 0) x else x - 2 * radius, if (dy > 0) y else y - 2 * radius),
            size = androidx.compose.ui.geometry.Size(2 * radius, 2 * radius),
            style = stroke,
        )
    }
    corner(left, top, 1f, 1f)
    corner(right, top, -1f, 1f)
    corner(left, bottom, 1f, -1f)
    corner(right, bottom, -1f, -1f)
}

@Composable
private fun TextReadyChip(modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .height(40.dp)
            .shadow(10.dp, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .padding(horizontal = 14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_text_aa), contentDescription = null, tint = Ember, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.onboarding_demo_text_ready),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** An "idea central" card over a dark flashcard with its three difficulty chips. */
@Composable
fun StudyIllustration(modifier: Modifier = Modifier) {
    IllustrationFrame(modifier) {
        Column(
            Modifier
                .offset(20.dp, 44.dp)
                .width(220.dp)
                .fadeUp(100)
                .rotate(-4f)
                .shadow(14.dp, CardShape)
                .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
                .padding(16.dp),
        ) {
            DemoLabel(R.drawable.ic_lightbulb, Color(0xFFE0A43A), stringResource(R.string.onboarding_demo_idea_label), Ember)
            Text(
                text = stringResource(R.string.onboarding_demo_idea),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Column(
            Modifier
                .offset(70.dp, 156.dp)
                .width(210.dp)
                .popIn(300)
                .rotate(3f)
                .shadow(18.dp, CardShape)
                .background(Cover, CardShape)
                .padding(16.dp),
        ) {
            DemoLabel(R.drawable.ic_cards_fill, Amber, stringResource(R.string.onboarding_demo_card_label), Amber)
            Text(
                text = stringResource(R.string.onboarding_demo_card_question),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Paper,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
                listOf(Ember, Amber, Color(0xFF7FC79E)).forEach {
                    Box(Modifier.weight(1f).height(20.dp).background(it.copy(alpha = 0.5f), CircleShape))
                }
            }
        }
        PulsingSparkle(x = 248, y = 30, size = 28)
        PulsingSparkle(x = 30, y = 190, size = 22, delayMillis = 700)
    }
}

private val CardShape = RoundedCornerShape(22.dp)

@Composable
private fun DemoLabel(icon: Int, iconTint: Color, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.12.em),
            fontSize = 11.sp,
        )
    }
}
