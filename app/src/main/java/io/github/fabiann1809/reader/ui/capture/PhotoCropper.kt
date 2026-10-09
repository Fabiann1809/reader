package io.github.fabiann1809.reader.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ocr.Corner
import io.github.fabiann1809.reader.ocr.ImageArea

private val DimColor = Color.Black.copy(alpha = 0.55f)

// The amber of the camera frame, so the crop looks like its continuation.
private val CropColor = Color(0xFFF5B963)
private val HandleRadius = 9.dp

// How close to a corner a finger must land to drag it instead of the whole area.
private val CornerTouchRadius = 32.dp

/**
 * The crop rectangle drawn over the photo (T12.5), sized exactly like the photo: the corners
 * resize it, a drag inside moves it, and while [pickingParagraph] the [paragraphs] are outlined
 * and a tap picks one.
 */
@Composable
fun PhotoCropper(
    area: ImageArea,
    paragraphs: List<ImageArea>?,
    pickingParagraph: Boolean,
    paragraphColor: Color,
    onMoveCorner: (Corner, Float, Float) -> Unit,
    onMoveArea: (Float, Float) -> Unit,
    onTapParagraph: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The gesture detectors outlive recompositions, so they read the latest area through this.
    val currentArea by rememberUpdatedState(area)
    val drag = remember { CropDrag() }
    val gestures = if (pickingParagraph) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { tap -> onTapParagraph(tap.x / size.width, tap.y / size.height) }
        }
    } else {
        Modifier.pointerInput(Unit) {
            val touchRadius = CornerTouchRadius.toPx()
            detectDragGestures(
                onDragStart = { start ->
                    val rect = currentArea.toRect(Size(size.width.toFloat(), size.height.toFloat()))
                    drag.corner = nearestCorner(rect, start, touchRadius)
                    drag.movesArea = drag.corner == null && rect.contains(start)
                },
                onDrag = { change, amount ->
                    change.consume()
                    val dx = amount.x / size.width
                    val dy = amount.y / size.height
                    val corner = drag.corner
                    when {
                        corner != null -> onMoveCorner(corner, dx, dy)
                        drag.movesArea -> onMoveArea(dx, dy)
                    }
                },
            )
        }
    }
    Canvas(modifier = modifier.testTag(PHOTO_CROPPER_TAG).then(gestures)) {
        if (pickingParagraph) {
            drawRect(DimColor)
            paragraphs.orEmpty().forEach { drawParagraph(it.toRect(size), paragraphColor) }
        } else {
            drawCrop(area.toRect(size))
        }
    }
}

const val PHOTO_CROPPER_TAG = "photo_cropper"

/** What the finger that is down is dragging: a corner, the whole area, or nothing. */
private class CropDrag {
    var corner: Corner? = null
    var movesArea = false
}

private fun ImageArea.toRect(size: Size) =
    Rect(left * size.width, top * size.height, right * size.width, bottom * size.height)

private fun Rect.cornerOffset(corner: Corner) = when (corner) {
    Corner.TOP_LEFT -> topLeft
    Corner.TOP_RIGHT -> topRight
    Corner.BOTTOM_LEFT -> bottomLeft
    Corner.BOTTOM_RIGHT -> bottomRight
}

private fun nearestCorner(rect: Rect, point: Offset, touchRadius: Float): Corner? =
    Corner.entries
        .map { it to (rect.cornerOffset(it) - point).getDistance() }
        .filter { (_, distance) -> distance <= touchRadius }
        .minByOrNull { (_, distance) -> distance }
        ?.first

/** Dims everything outside [crop] and draws its border and corner handles. */
private fun DrawScope.drawCrop(crop: Rect) {
    // One dim layer with a hole: four separate rectangles would leave faint seams where they meet.
    clipRect(crop.left, crop.top, crop.right, crop.bottom, clipOp = ClipOp.Difference) { drawRect(DimColor) }
    drawRect(CropColor, topLeft = crop.topLeft, size = crop.size, style = Stroke(width = 2.dp.toPx()))
    Corner.entries.forEach { drawCircle(CropColor, radius = HandleRadius.toPx(), center = crop.cornerOffset(it)) }
}

private fun DrawScope.drawParagraph(paragraph: Rect, color: Color) {
    drawRect(color.copy(alpha = 0.25f), topLeft = paragraph.topLeft, size = paragraph.size)
    drawRect(color, topLeft = paragraph.topLeft, size = paragraph.size, style = Stroke(width = 2.dp.toPx()))
}
