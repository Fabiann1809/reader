package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.prefs.ReadingAlignment
import io.github.fabiann1809.reader.data.prefs.ReadingFont
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.prefs.ReadingTheme
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

/** Page and text colors of each reader theme (design 2.3), as ARGB. */
data class ReadingColors(val background: Long, val text: Long, val isDark: Boolean)

fun ReadingTheme.colors(): ReadingColors = when (this) {
    ReadingTheme.DAY -> ReadingColors(background = 0xFFFBF8F1, text = 0xFF2A2320, isDark = false)
    ReadingTheme.SEPIA -> ReadingColors(background = 0xFFF1E4C8, text = 0xFF4B3A26, isDark = false)
    ReadingTheme.PAPER_GRAY -> ReadingColors(background = 0xFFE8E8E4, text = 0xFF222222, isDark = false)
    ReadingTheme.NIGHT -> ReadingColors(background = 0xFF1B1B1A, text = 0xFFD9D3C9, isDark = true)
    ReadingTheme.AMOLED -> ReadingColors(background = 0xFF000000, text = 0xFFC9C4BA, isDark = true)
    // Its colors live in the settings: see ReadingSettings.pageColors().
    ReadingTheme.CUSTOM -> ReadingColors(
        background = ReadingSettings.DEFAULT_CUSTOM_BACKGROUND,
        text = ReadingSettings.DEFAULT_CUSTOM_TEXT,
        isDark = false,
    )
}

// The CSS family name; OpenDyslexic is the one Readium already bundles.
private val ReadingFont.familyName: String
    get() = when (this) {
        ReadingFont.LITERATA -> "Literata"
        ReadingFont.MERRIWEATHER -> "Merriweather"
        ReadingFont.SOURCE_SERIF -> "Source Serif 4"
        ReadingFont.LORA -> "Lora"
        ReadingFont.ATKINSON -> "Atkinson Hyperlegible"
        ReadingFont.OPEN_DYSLEXIC -> "OpenDyslexic"
        ReadingFont.INTER -> "Inter"
    }

// The file prefix in assets/fonts (latin subsets, enough for Spanish).
private val ReadingFont.fileName: String?
    get() = when (this) {
        ReadingFont.LITERATA -> "literata"
        ReadingFont.MERRIWEATHER -> "merriweather"
        ReadingFont.SOURCE_SERIF -> "source-serif-4"
        ReadingFont.LORA -> "lora"
        ReadingFont.ATKINSON -> "atkinson-hyperlegible"
        ReadingFont.OPEN_DYSLEXIC -> null
        ReadingFont.INTER -> "inter"
    }

/**
 * The navigator's preferences for these settings. [fontSizeOverride] is a pinch's size while
 * reading. Publisher styles are off so the line height and alignment take effect.
 */
@OptIn(ExperimentalReadiumApi::class)
fun ReadingSettings.toEpubPreferences(fontSizeOverride: Double?): EpubPreferences {
    val colors = pageColors()
    return EpubPreferences(
        theme = if (colors.isDark) Theme.DARK else Theme.LIGHT,
        backgroundColor = Color(colors.background.toInt()),
        textColor = Color(colors.text.toInt()),
        fontFamily = FontFamily(font.familyName),
        fontSize = fontSizeOverride ?: fontSize,
        lineHeight = lineHeight,
        pageMargins = margins,
        textAlign = if (alignment == ReadingAlignment.JUSTIFY) TextAlign.JUSTIFY else TextAlign.START,
        publisherStyles = false,
    )
}

/** Serves the bundled reading fonts to the book's pages, in regular, bold and italic. */
@OptIn(ExperimentalReadiumApi::class)
fun readerFontsConfiguration(): EpubNavigatorFragment.Configuration = EpubNavigatorFragment.Configuration().apply {
    servedAssets += "fonts/.*"
    ReadingFont.entries.forEach { font ->
        val file = font.fileName ?: return@forEach
        addFontFamilyDeclaration(FontFamily(font.familyName)) {
            for (weight in listOf(FontWeight.NORMAL, FontWeight.BOLD)) {
                for (style in FontStyle.entries) {
                    addFontFace {
                        addSource("fonts/$file-latin-${weight.value}-${style.name.lowercase()}.woff2")
                        setFontWeight(weight)
                        setFontStyle(style)
                    }
                }
            }
        }
    }
}
