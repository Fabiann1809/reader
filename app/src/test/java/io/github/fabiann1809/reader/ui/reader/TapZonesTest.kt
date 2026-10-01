package io.github.fabiann1809.reader.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapZonesTest {

    @Test
    fun theMiddleFortyPercentIsTheCenter() {
        assertTrue(isCenterTap(x = 540f, width = 1080))
        assertTrue(isCenterTap(x = 324f, width = 1080))
        assertTrue(isCenterTap(x = 756f, width = 1080))
    }

    @Test
    fun theSidesAreNotTheCenter() {
        assertFalse(isCenterTap(x = 100f, width = 1080))
        assertFalse(isCenterTap(x = 1000f, width = 1080))
    }

    @Test
    fun aPageWithoutWidthHasNoCenter() {
        assertFalse(isCenterTap(x = 0f, width = 0))
    }
}
