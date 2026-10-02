package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.prefs.ReadingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPaletteTest {

    @Test
    fun customUsesThePickedColors() {
        val settings = ReadingSettings(theme = ReadingTheme.CUSTOM, customBackground = 0xFFE6F5EC, customText = 0xFF22533D)

        val colors = settings.pageColors()

        assertEquals(0xFFE6F5EC, colors.background)
        assertEquals(0xFF22533D, colors.text)
        assertFalse(colors.isDark)
    }

    @Test
    fun aDarkCustomPageIsDark() {
        assertTrue(ReadingSettings(theme = ReadingTheme.CUSTOM, customBackground = 0xFF2E1B10).pageColors().isDark)
    }

    @Test
    fun theOtherThemesIgnoreTheCustomColors() {
        val settings = ReadingSettings(theme = ReadingTheme.SEPIA, customBackground = 0xFF000000)

        assertEquals(ReadingTheme.SEPIA.colors(), settings.pageColors())
    }

    @Test
    fun bothPalettesComeFromTheDesign() {
        assertTrue(ReadingSettings.DEFAULT_CUSTOM_BACKGROUND in CustomBackgrounds)
        assertTrue(ReadingSettings.DEFAULT_CUSTOM_TEXT in CustomTexts)
    }
}
