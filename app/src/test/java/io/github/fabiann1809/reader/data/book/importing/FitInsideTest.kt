package io.github.fabiann1809.reader.data.book.importing

import org.junit.Assert.assertEquals
import org.junit.Test

class FitInsideTest {

    private val max = PixelSize(480, 720)

    @Test
    fun aLargeCoverIsScaledDownKeepingItsProportions() {
        assertEquals(PixelSize(480, 720), fitInside(1200, 1800, max))
        // A wide page is limited by the width.
        assertEquals(PixelSize(480, 360), fitInside(1600, 1200, max))
    }

    @Test
    fun aSmallCoverIsNeverScaledUp() {
        assertEquals(PixelSize(200, 300), fitInside(200, 300, max))
    }

    @Test
    fun aVectorPageCanBeEnlarged() {
        // An A6 PDF page (298 x 420 points) fills the cover height.
        assertEquals(PixelSize(511, 720), fitInside(298, 420, PixelSize(600, 720), canEnlarge = true))
    }

    @Test
    fun aTinySideNeverRoundsToZero() {
        assertEquals(PixelSize(480, 1), fitInside(10_000, 2, max))
    }
}
