package io.github.fabiann1809.reader.ui.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.prefs.ReadingAlignment
import io.github.fabiann1809.reader.data.prefs.ReadingFont
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.prefs.ReadingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme

/** Instrumented because Readium's Theme reads Android colors when it loads. */
@RunWith(AndroidJUnit4::class)
class EpubStyleTest {

    @Test
    fun theSettingsBecomeTheNavigatorsPreferences() {
        val settings = ReadingSettings(
            theme = ReadingTheme.SEPIA,
            font = ReadingFont.SOURCE_SERIF,
            fontSize = 1.4,
            lineHeight = 2.0,
            margins = 1.5,
            alignment = ReadingAlignment.JUSTIFY,
        )

        val preferences = settings.toEpubPreferences(fontSizeOverride = null)

        assertEquals(0xFFF1E4C8.toInt(), preferences.backgroundColor?.int)
        assertEquals(0xFF4B3A26.toInt(), preferences.textColor?.int)
        assertEquals(Theme.LIGHT, preferences.theme)
        assertEquals(FontFamily("Source Serif 4"), preferences.fontFamily)
        assertEquals(1.4, preferences.fontSize!!, 0.0001)
        assertEquals(2.0, preferences.lineHeight!!, 0.0001)
        assertEquals(1.5, preferences.pageMargins!!, 0.0001)
        assertEquals(TextAlign.JUSTIFY, preferences.textAlign)
        // Off, or Readium would ignore the line height and the alignment.
        assertFalse(preferences.publisherStyles!!)
    }

    @Test
    fun aPinchSizeWinsWhileReading() {
        val preferences = ReadingSettings(fontSize = 1.0).toEpubPreferences(fontSizeOverride = 2.2)

        assertEquals(2.2, preferences.fontSize!!, 0.0001)
    }

    @Test
    fun customSendsThePickedColorsAndADarkPageIsDark() {
        val light = ReadingSettings(theme = ReadingTheme.CUSTOM, customBackground = 0xFFF3F0FD, customText = 0xFF4A403A)
        val dark = ReadingSettings(theme = ReadingTheme.CUSTOM, customBackground = 0xFF2E1B10, customText = 0xFFD9D3C9)

        assertEquals(0xFFF3F0FD.toInt(), light.toEpubPreferences(null).backgroundColor?.int)
        assertEquals(0xFF4A403A.toInt(), light.toEpubPreferences(null).textColor?.int)
        assertEquals(Theme.LIGHT, light.toEpubPreferences(null).theme)
        assertEquals(Theme.DARK, dark.toEpubPreferences(null).theme)
    }

    @Test
    fun nightAndAmoledAreDarkThemes() {
        assertEquals(Theme.DARK, ReadingSettings(theme = ReadingTheme.NIGHT).toEpubPreferences(null).theme)
        assertEquals(0xFF000000.toInt(), ReadingSettings(theme = ReadingTheme.AMOLED).toEpubPreferences(null).backgroundColor?.int)
    }
}
