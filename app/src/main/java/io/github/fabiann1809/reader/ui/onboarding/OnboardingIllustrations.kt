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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.rememberEntranceProgress
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

// Illustrations of the three onboarding steps, drawn with the app's own tokens (no images).

private const val BOOK_STAGGER_MILLIS = 100
private const val SHELF_ENTRANCE_MILLIS = 600
private const val SCAN_MILLIS = 1_800
private val PaperColor = Color(0xFFEFE6D6)
private val ScanAmber = Color(0xFFF5B963)
private val ScannerBackground = Color(0xFF1E1915)

private class ShelfBook(val width: Int, val height: Int, val cover: Color, val band: Color)

private val ShelfBooks = listOf(
    ShelfBook(66, 116, Color(0xFF2F4858), Color(0xFFE0A458)),
    ShelfBook(62, 104, Color(0xFFB9473A), Color(0xFFF2C14E)),
    ShelfBook(66, 122, Color(0xFF5B4B8A), Color(0xFFE7B7C8)),
    ShelfBook(58, 98, Color(0xFF3E5C47), Color(0xFFD9B26F)),
)

/** Four books standing on a shelf of the library wall. */
@Composable
fun ShelfIllustration(modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Box(modifier.fillMaxWidth().height(ILLUSTRATION_HEIGHT.dp).background(colors.woodWall)) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(WALL_EDGE.dp).fillMaxSize().background(colors.woodGrain))
            Box(Modifier.weight(1f))
            Box(Modifier.width(WALL_EDGE.dp).fillMaxSize().background(colors.woodGrain))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 84.dp),
        ) {
            ShelfBooks.forEachIndexed { index, book -> DroppingBook(book, delayMillis = BOOK_STAGGER_MILLIS * (index + 1)) }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 54.dp)
                .fillMaxWidth()
                .height(9.dp)
                .background(Brush.verticalGradient(colors.shelfTop)),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .height(22.dp)
                .background(Brush.verticalGradient(colors.shelfFront)),
        )
    }
}

@Composable
private fun DroppingBook(book: ShelfBook, delayMillis: Int) {
    val progress = rememberEntranceProgress(delayMillis, SHELF_ENTRANCE_MILLIS, Motion.PopEasing)
    Box(
        Modifier
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
                translationY = (1f - progress) * -26.dp.toPx()
                rotationZ = (1f - progress) * -4f
            }
            .size(book.width.dp, book.height.dp)
            .shadow(6.dp, BookShape)
            .clip(BookShape)
            .background(book.cover),
    ) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(book.band))
    }
}

private val BookShape = RoundedCornerShape(topStart = 3.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 3.dp)

/** A tilted page under a scanning line and the four corners of the capture frame. */
@Composable
fun ScanIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(ILLUSTRATION_HEIGHT.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(ScannerBackground),
    ) {
        Text(
            text = stringResource(R.string.onboarding_demo_paper),
            color = Color(0xFF4D4038),
            fontFamily = FontFamily.Serif,
            fontSize = 10.sp,
            lineHeight = 17.sp,
            modifier = Modifier
                .padding(44.dp)
                .rotate(-3f)
                .background(PaperColor, RoundedCornerShape(6.dp))
                .padding(18.dp),
        )
        ScanLine()
        Canvas(Modifier.fillMaxSize().padding(22.dp)) { drawFrameCorners() }
    }
}

@Composable
private fun BoxScope.ScanLine() {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "scan")
    val animated by transition.animateFloat(
        initialValue = SCAN_START,
        targetValue = SCAN_END,
        animationSpec = infiniteRepeatable(tween(SCAN_MILLIS, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanPosition",
    )
    val position = if (reduceMotion) 0.5f else animated
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .padding(horizontal = 34.dp)
            .fillMaxWidth()
            .height(14.dp)
            .offset(y = (ILLUSTRATION_HEIGHT * position).dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, ScanAmber, Color.Transparent))),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrameCorners() {
    val arm = 30.dp.toPx()
    val radius = 14.dp.toPx()
    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    val corner = Path().apply {
        moveTo(0f, arm)
        lineTo(0f, radius)
        quadraticTo(0f, 0f, radius, 0f)
        lineTo(arm, 0f)
    }
    listOf(
        Offset(0f, 0f) to Offset(1f, 1f),
        Offset(size.width, 0f) to Offset(-1f, 1f),
        Offset(0f, size.height) to Offset(1f, -1f),
        Offset(size.width, size.height) to Offset(-1f, -1f),
    ).forEach { (origin, flip) ->
        withTransform({
            translate(origin.x, origin.y)
            scale(flip.x, flip.y, pivot = Offset.Zero)
        }) { drawPath(corner, ScanAmber, style = stroke) }
    }
}

/** A pastel "central idea" card over a tilted flashcard with its three difficulty chips. */
@Composable
fun StudyIllustration(modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val ideaIn = rememberEntranceProgress(delayMillis = 100, durationMillis = 500)
    val cardIn = rememberEntranceProgress(delayMillis = 300, durationMillis = 500, easing = Motion.PopEasing)
    Box(modifier.fillMaxWidth().height(ILLUSTRATION_HEIGHT.dp)) {
        Column(
            Modifier
                .padding(top = 20.dp, end = 30.dp)
                .graphicsLayer {
                    alpha = ideaIn
                    translationY = (1f - ideaIn) * 16.dp.toPx()
                }
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.horizontalGradient(colors.pastels[0]))
                .padding(18.dp),
        ) {
            DemoLabel(stringResource(R.string.onboarding_demo_idea_label), colors.onPastel)
            Text(
                text = stringResource(R.string.onboarding_demo_idea),
                color = colors.onPastel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, start = 40.dp)
                .graphicsLayer {
                    alpha = cardIn.coerceIn(0f, 1f)
                    scaleX = 0.88f + 0.12f * cardIn
                    scaleY = 0.88f + 0.12f * cardIn
                }
                .rotate(3f)
                .fillMaxWidth()
                .height(130.dp)
                .shadow(18.dp, CardShape)
                .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
                .padding(18.dp),
        ) {
            DemoLabel(stringResource(R.string.onboarding_demo_card_label), MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.onboarding_demo_card_question),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                listOf(
                    MaterialTheme.colorScheme.errorContainer,
                    colors.warningContainer,
                    colors.successContainer,
                ).forEach { Box(Modifier.size(50.dp, 24.dp).background(it, CircleShape)) }
            }
        }
    }
}

private val CardShape = RoundedCornerShape(26.dp)

@Composable
private fun DemoLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.14.em),
    )
}

private const val ILLUSTRATION_HEIGHT = 300
private const val WALL_EDGE = 30
private const val SCAN_START = 0.04f
private const val SCAN_END = 0.88f
