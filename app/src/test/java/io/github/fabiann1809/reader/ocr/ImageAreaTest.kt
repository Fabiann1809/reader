package io.github.fabiann1809.reader.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageAreaTest {

    private val area = ImageArea(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f)

    private fun assertArea(expected: ImageArea, actual: ImageArea) {
        assertEquals(expected.left, actual.left, 0.0001f)
        assertEquals(expected.top, actual.top, 0.0001f)
        assertEquals(expected.right, actual.right, 0.0001f)
        assertEquals(expected.bottom, actual.bottom, 0.0001f)
    }

    @Test
    fun draggingACornerMovesOnlyItsTwoSides() {
        assertArea(ImageArea(0.1f, 0.3f, 0.8f, 0.8f), area.withCornerMoved(Corner.TOP_LEFT, dx = -0.1f, dy = 0.1f, minSize = 0.05f))
        assertArea(ImageArea(0.2f, 0.2f, 0.9f, 0.7f), area.withCornerMoved(Corner.BOTTOM_RIGHT, dx = 0.1f, dy = -0.1f, minSize = 0.05f))
    }

    @Test
    fun aCornerStaysInsideTheImageAndCantCrossTheOtherSide() {
        assertArea(ImageArea(0f, 0f, 0.8f, 0.8f), area.withCornerMoved(Corner.TOP_LEFT, dx = -1f, dy = -1f, minSize = 0.05f))
        assertArea(ImageArea(0.75f, 0.2f, 0.8f, 0.8f), area.withCornerMoved(Corner.TOP_LEFT, dx = 0.9f, dy = 0f, minSize = 0.05f))
    }

    @Test
    fun theWholeAreaMovesWithoutLeavingTheImage() {
        assertArea(ImageArea(0.4f, 0.1f, 1f, 0.7f), area.movedBy(dx = 0.5f, dy = -0.1f))
    }

    @Test
    fun aParagraphIsGrownALittleAndKeptInside() {
        assertArea(ImageArea(0f, 0.45f, 0.62f, 0.62f), ImageArea(0.01f, 0.47f, 0.6f, 0.6f).grownBy(0.02f))
    }

    @Test
    fun onlyTheFullRectangleIsTheWholeImage() {
        assertTrue(ImageArea.WholeImage.isWholeImage)
        assertFalse(area.isWholeImage)
    }
}
