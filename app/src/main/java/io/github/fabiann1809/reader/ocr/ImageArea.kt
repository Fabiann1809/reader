package io.github.fabiann1809.reader.ocr

/**
 * A rectangle on an image as fractions of its width and height (0 to 1), so it means the same
 * on the small preview and on the full photo (T12.5).
 */
data class ImageArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    val isWholeImage: Boolean get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

    /** The same area grown by [margin] on every side, kept inside the image (a paragraph needs some air). */
    fun grownBy(margin: Float): ImageArea = ImageArea(
        left = (left - margin).coerceAtLeast(0f),
        top = (top - margin).coerceAtLeast(0f),
        right = (right + margin).coerceAtMost(1f),
        bottom = (bottom + margin).coerceAtMost(1f),
    )

    companion object {
        val WholeImage = ImageArea(0f, 0f, 1f, 1f)
    }
}

/** A corner of the crop rectangle, which the finger drags. */
enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * The area after dragging [corner] by ([dx], [dy]) fractions: it stays inside the image and
 * never gets smaller than [minSize] on either side (or turns inside out).
 */
fun ImageArea.withCornerMoved(corner: Corner, dx: Float, dy: Float, minSize: Float): ImageArea {
    val movesLeft = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT
    val movesTop = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT
    val newLeft = if (movesLeft) (left + dx).coerceIn(0f, right - minSize) else left
    val newRight = if (movesLeft) right else (right + dx).coerceIn(left + minSize, 1f)
    val newTop = if (movesTop) (top + dy).coerceIn(0f, bottom - minSize) else top
    val newBottom = if (movesTop) bottom else (bottom + dy).coerceIn(top + minSize, 1f)
    return ImageArea(newLeft, newTop, newRight, newBottom)
}

/** The area moved as a whole by ([dx], [dy]) fractions, without leaving the image. */
fun ImageArea.movedBy(dx: Float, dy: Float): ImageArea {
    val x = dx.coerceIn(-left, 1f - right)
    val y = dy.coerceIn(-top, 1f - bottom)
    return ImageArea(left + x, top + y, right + x, bottom + y)
}
