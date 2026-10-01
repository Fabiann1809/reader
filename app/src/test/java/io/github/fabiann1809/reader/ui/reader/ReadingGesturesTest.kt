package io.github.fabiann1809.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingGesturesTest {

    @Test
    fun onlyTheLeftStripIsTheBrightnessEdge() {
        assertTrue(isBrightnessEdge(x = 50f, width = 1080))
        assertFalse(isBrightnessEdge(x = 200f, width = 1080))
        assertFalse(isBrightnessEdge(x = 0f, width = 0))
    }

    @Test
    fun draggingUpBrightensAndDownDims() {
        assertEquals(0.7f, brightnessAfterDrag(start = 0.5f, dragFraction = -0.2f), 0.0001f)
        assertEquals(0.3f, brightnessAfterDrag(start = 0.5f, dragFraction = 0.2f), 0.0001f)
    }

    @Test
    fun brightnessNeverGoesBlackNorPastFull() {
        assertEquals(0.02f, brightnessAfterDrag(start = 0.1f, dragFraction = 0.5f), 0.0001f)
        assertEquals(1f, brightnessAfterDrag(start = 0.9f, dragFraction = -0.5f), 0.0001f)
    }

    @Test
    fun pinchingScalesTheFontWithinLimits() {
        assertEquals(1.5, fontSizeAfterPinch(current = 1.0, scale = 1.5f), 0.0001)
        assertEquals(0.5, fontSizeAfterPinch(current = 0.6, scale = 0.5f), 0.0001)
        assertEquals(3.0, fontSizeAfterPinch(current = 2.5, scale = 2f), 0.0001)
    }
}
