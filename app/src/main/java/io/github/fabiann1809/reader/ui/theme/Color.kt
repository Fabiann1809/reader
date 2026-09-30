package io.github.fabiann1809.reader.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens from the Reader design system (04-system-design.md, section 2).
// Screens should use MaterialTheme.colorScheme or ReaderTheme.colors, not these values directly.

// Forest green (primary)
val Primary30 = Color(0xFF22533D)
val Primary40 = Color(0xFF2F6B4F)
val Primary60 = Color(0xFF4C9A73)
val Primary80 = Color(0xFFA9D6BE)
val Primary90 = Color(0xFFCFEBDC)
val Primary95 = Color(0xFFE6F5EC)

// Wood (library backdrop)
val Wood900 = Color(0xFF2E1B10)
val Wood800 = Color(0xFF4A2C1A)
val Wood700 = Color(0xFF6B4226)
val Wood600 = Color(0xFF8A5A3B)
val Wood400 = Color(0xFFC4915F)
val ShelfTopA = Color(0xFFF0BE72)
val ShelfTopB = Color(0xFFD19A4B)
val ShelfFrontA = Color(0xFFC98F44)
val ShelfFrontB = Color(0xFFA66F2E)

// Warm neutrals
val Paper50 = Color(0xFFFFFBF5)
val Paper100 = Color(0xFFFBF6EE)
val Paper200 = Color(0xFFF1E8DA)
val Paper300 = Color(0xFFE2D6C3)
val Ink900 = Color(0xFF2A2320)
val Ink700 = Color(0xFF4A403A)
val Ink500 = Color(0xFF7A6E65)
val Ink300 = Color(0xFFB5A99E)

// AI accent (lavender): reserved for AI features so users can tell them apart.
val Ai40 = Color(0xFF6F63B5)
val Ai80 = Color(0xFFC8BFF5)
val Ai90 = Color(0xFFE6E1FA)
val Ai95 = Color(0xFFF3F0FD)

// Semantic
val Success = Color(0xFF3E8E63)
val Warning = Color(0xFFD99A2B)
val Error = Color(0xFFC5483A)
val Info = Color(0xFF3B7EA1)

// Dark mode surfaces (section 2.2)
val DarkSurface = Color(0xFF161311)
val DarkSurfaceContainer = Color(0xFF221E1B)
val DarkSurfaceContainerHigh = Color(0xFF2C2723)
val DarkOnSurface = Color(0xFFEFE6DA)
val DarkOnPrimary = Color(0xFF0F3222)

// Muted colors for generated covers, picked by title hash (section 2.4).
val CoverPalette = listOf(
    Color(0xFF3F5B7A),
    Color(0xFF7A3F4B),
    Color(0xFF4E6B4A),
    Color(0xFF8A6A3A),
    Color(0xFF5A4A7A),
    Color(0xFF3F6E6E),
    Color(0xFF7A4F3A),
    Color(0xFF5B5B5B),
)
