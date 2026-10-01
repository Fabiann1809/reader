package io.github.fabiann1809.reader.ui.reader

// The page is split in three: 30 % on each side and 40 % in the middle (design 03 §4).
private const val SIDE_ZONE_FRACTION = 0.3f

/** True when a tap at [x] (in pixels) on a page [width] pixels wide falls in the middle zone, which shows or hides the controls. */
fun isCenterTap(x: Float, width: Int): Boolean {
    if (width <= 0) return false
    val fraction = x / width
    return fraction >= SIDE_ZONE_FRACTION && fraction <= 1f - SIDE_ZONE_FRACTION
}
