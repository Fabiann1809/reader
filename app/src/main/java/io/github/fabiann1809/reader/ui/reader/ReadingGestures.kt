package io.github.fabiann1809.reader.ui.reader

// The strip on the left where a vertical drag changes the brightness.
private const val BRIGHTNESS_EDGE_FRACTION = 0.1f

// Never fully black, so the page can always be found again.
private const val MIN_BRIGHTNESS = 0.02f

// Readium's font size is a multiplier of the book's own size (1.0 = as published).
private const val MIN_FONT_SIZE = 0.5
private const val MAX_FONT_SIZE = 3.0

/** True when a touch at [x] (in pixels) on a page [width] pixels wide starts on the brightness edge. */
fun isBrightnessEdge(x: Float, width: Int): Boolean = width > 0 && x <= width * BRIGHTNESS_EDGE_FRACTION

/**
 * The brightness (0 to 1) after a vertical drag of [dragFraction] of the page's height: dragging
 * up (negative) brightens, dragging down dims, as on most readers.
 */
fun brightnessAfterDrag(start: Float, dragFraction: Float): Float = (start - dragFraction).coerceIn(MIN_BRIGHTNESS, 1f)

/** The font size after a pinch that scaled the page by [scale]: spreading the fingers makes the text bigger. */
fun fontSizeAfterPinch(current: Double, scale: Float): Double = (current * scale).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
