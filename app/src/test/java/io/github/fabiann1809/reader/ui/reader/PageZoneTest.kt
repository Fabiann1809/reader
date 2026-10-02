package io.github.fabiann1809.reader.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageZoneTest {

    @Test
    fun aZoneCanBeDrawnInAnyDirection() {
        val zone = PageZone.between(startX = 300f, startY = 500f, endX = 100f, endY = 200f)

        assertEquals(PageZone(left = 100f, top = 200f, right = 300f, bottom = 500f), zone)
        assertEquals(200f, zone.width, 0.001f)
        assertEquals(300f, zone.height, 0.001f)
    }

    @Test
    fun aTinyZoneIsNotUsable() {
        assertFalse(PageZone.between(0f, 0f, 20f, 200f).isUsable(minSize = 48f))
        assertTrue(PageZone.between(0f, 0f, 400f, 120f).isUsable(minSize = 48f))
    }
}
