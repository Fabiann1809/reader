package io.github.fabiann1809.reader.data.prefs

/**
 * Page colors of the reader, independent of the app's theme (design 2.3). [CUSTOM] uses the page
 * and text colors the reader picked from a palette.
 */
enum class ReadingTheme { DAY, SEPIA, PAPER_GRAY, NIGHT, AMOLED, CUSTOM }

/** Fonts the reader offers (all OFL); Literata is the reader's default (design 3). */
enum class ReadingFont { LITERATA, MERRIWEATHER, SOURCE_SERIF, LORA, ATKINSON, OPEN_DYSLEXIC, INTER }

enum class ReadingAlignment { START, JUSTIFY }

/**
 * The "Aa" settings, saved for every book. [fontSize] is a multiplier of the book's own size,
 * [lineHeight] a multiple of the font size and [margins] Readium's page margin factor. The
 * discreet indicators under the page (T11.9) and keeping the screen on are page settings.
 */
data class ReadingSettings(
    val theme: ReadingTheme = ReadingTheme.DAY,
    val font: ReadingFont = ReadingFont.LITERATA,
    val fontSize: Double = DEFAULT_FONT_SIZE,
    val lineHeight: Double = DEFAULT_LINE_HEIGHT,
    val margins: Double = DEFAULT_MARGINS,
    val alignment: ReadingAlignment = ReadingAlignment.START,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val showPage: Boolean = true,
    val keepScreenOn: Boolean = false,
    // The "Personalizado" theme's colors, as ARGB.
    val customBackground: Long = DEFAULT_CUSTOM_BACKGROUND,
    val customText: Long = DEFAULT_CUSTOM_TEXT,
) {
    companion object {
        const val DEFAULT_FONT_SIZE = 1.0
        const val DEFAULT_LINE_HEIGHT = 1.6
        const val DEFAULT_MARGINS = 1.0

        // paper-50 and ink-900 (design 2.1).
        const val DEFAULT_CUSTOM_BACKGROUND = 0xFFFFFBF5
        const val DEFAULT_CUSTOM_TEXT = 0xFF2A2320

        val FontSizeRange = 0.5..3.0

        // Up to 2.5 for readability (design 9, accessibility).
        val LineHeightRange = 1.0..2.5
        val MarginsRange = 0.5..2.5
    }
}
