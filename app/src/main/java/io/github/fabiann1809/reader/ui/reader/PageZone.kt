package io.github.fabiann1809.reader.ui.reader

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A rectangle the reader marked on a PDF page (T11.14), in pixels from the page's top left corner. */
data class PageZone(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        /** The zone between where the finger went down and where it is now, in any direction. */
        fun between(startX: Float, startY: Float, endX: Float, endY: Float) = PageZone(
            left = min(startX, endX),
            top = min(startY, endY),
            right = max(startX, endX),
            bottom = max(startY, endY),
        )
    }
}

/** True when the zone is big enough to hold a line of text (both sides at least [minSize] pixels). */
fun PageZone.isUsable(minSize: Float): Boolean = abs(width) >= minSize && abs(height) >= minSize
