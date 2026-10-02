package io.github.fabiann1809.reader.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import io.github.fabiann1809.reader.data.prefs.ReadingTheme

// "Personalizado" picks from these, all taken from the design's colors (2.1 and 2.3), as ARGB.
val CustomBackgrounds: List<Long> = listOf(
    0xFFFFFBF5, // paper-50
    0xFFF1E8DA, // paper-200
    0xFFF1E4C8, // sepia page
    0xFFE6F5EC, // primary-95
    0xFFF3F0FD, // ai-95
    0xFFE8E8E4, // paper gray page
    0xFF1B1B1A, // night page
    0xFF2E1B10, // wood-900
    0xFF000000, // AMOLED page
)

val CustomTexts: List<Long> = listOf(
    0xFF2A2320, // ink-900
    0xFF4A403A, // ink-700
    0xFF4B3A26, // sepia text
    0xFF22533D, // primary-30
    0xFFD9D3C9, // night text
    0xFFFFFBF5, // paper-50
)

// Below this luminance a page counts as dark (Readium's dark mode, the capsule's color).
private const val DARK_LUMINANCE = 0.5f

/** The page's colors: the theme's, or the picked ones for "Personalizado". */
fun ReadingSettings.pageColors(): ReadingColors =
    if (theme == ReadingTheme.CUSTOM) {
        ReadingColors(customBackground, customText, isDark = Color(customBackground).luminance() < DARK_LUMINANCE)
    } else {
        theme.colors()
    }
