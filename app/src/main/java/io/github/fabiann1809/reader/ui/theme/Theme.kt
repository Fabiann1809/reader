package io.github.fabiann1809.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tonal buttons use secondaryContainer; AI elements use tertiary (see ReaderColors for the rest).
private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightAccentInk,
    inversePrimary = DarkAccent,
    secondary = LightAccentInk,
    onSecondary = LightOnAccent,
    secondaryContainer = LightChip,
    onSecondaryContainer = LightInk2,
    tertiary = LightAi,
    onTertiary = LightOnAi,
    tertiaryContainer = LightAiSoft,
    onTertiaryContainer = LightAiInk,
    background = LightBg,
    onBackground = LightInk,
    surface = LightBg,
    onSurface = LightInk,
    surfaceVariant = LightChip,
    onSurfaceVariant = LightMuted,
    surfaceTint = LightAccent,
    inverseSurface = LightInk,
    inverseOnSurface = LightSurface,
    error = LightErr,
    onError = Color.White,
    errorContainer = LightErrSoft,
    onErrorContainer = LightErrInk,
    outline = LightMuted,
    outlineVariant = LightLine,
    surfaceBright = LightSurface,
    surfaceDim = LightChip,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightChip,
    surfaceContainerHighest = LightChip,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkAccentInk,
    inversePrimary = LightAccent,
    secondary = DarkAccentInk,
    onSecondary = DarkOnAccent,
    secondaryContainer = DarkChip,
    onSecondaryContainer = DarkInk2,
    tertiary = DarkAi,
    onTertiary = DarkOnAi,
    tertiaryContainer = DarkAiSoft,
    onTertiaryContainer = DarkAiInk,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkBg,
    onSurface = DarkInk,
    surfaceVariant = DarkChip,
    onSurfaceVariant = DarkMuted,
    surfaceTint = DarkAccent,
    inverseSurface = DarkInk,
    inverseOnSurface = DarkBg,
    error = DarkErr,
    onError = DarkOnAccent,
    errorContainer = DarkErrSoft,
    onErrorContainer = DarkErrInk,
    outline = DarkMuted,
    outlineVariant = DarkLine,
    surfaceBright = DarkSurface,
    surfaceDim = DarkChip,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkChip,
    surfaceContainerHighest = DarkChip,
)

/** Design colors that have no Material 3 role: AI accent, library wall and shelves, and semantic states. */
@Immutable
data class ReaderColors(
    val ai: Color,
    val aiContainer: Color,
    val aiSoft: Color,
    val woodWall: Color,
    val woodGrain: Color,
    val shelfTop: List<Color>,
    val shelfFront: List<Color>,
    val progress: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val navBar: Color,
    val navActive: Color,
    val pastels: List<List<Color>>,
    val onPastel: Color,
    val covers: List<CoverStyle>,
)

private val LightReaderColors = ReaderColors(
    ai = LightAi,
    aiContainer = LightAiSoft,
    aiSoft = LightAiSoft,
    woodWall = LightWall,
    woodGrain = LightWallShade,
    shelfTop = listOf(Color(0xFF6C5546), LightShelfTop),
    shelfFront = listOf(Color(0xFF33261F), LightShelfFront),
    progress = LightAccent,
    success = LightOk,
    successContainer = LightOkSoft,
    onSuccessContainer = LightOkInk,
    warning = LightWarn,
    warningContainer = LightWarnSoft,
    onWarningContainer = LightWarnInk,
    navBar = LightNavBar,
    navActive = LightNavActive,
    pastels = LightPastels,
    onPastel = LightInk,
    covers = CoverStyles,
)

private val DarkReaderColors = LightReaderColors.copy(
    ai = DarkAi,
    aiContainer = DarkAiSoft,
    aiSoft = DarkAiSoft,
    woodWall = DarkWall,
    woodGrain = DarkWallShade,
    shelfTop = listOf(Color(0xFF574334), DarkShelfTop),
    shelfFront = listOf(Color(0xFF1C1411), DarkShelfFront),
    progress = DarkAccent,
    success = DarkOk,
    successContainer = DarkOkSoft,
    onSuccessContainer = DarkOkInk,
    warning = DarkWarn,
    warningContainer = DarkWarnSoft,
    onWarningContainer = DarkWarnInk,
    navBar = DarkNavBar,
    navActive = DarkNavActive,
    pastels = DarkPastels,
    onPastel = DarkInk,
)

private val LocalReaderColors = staticCompositionLocalOf { LightReaderColors }

// Radii of the prototype: chips and buttons are pills (CircleShape), the rest uses these.
private val ReaderShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/** Accessors for the design colors that MaterialTheme doesn't cover. */
object ReaderTheme {
    val colors: ReaderColors
        @Composable
        @ReadOnlyComposable
        get() = LocalReaderColors.current
}

// Dynamic (wallpaper) color is intentionally not used: the terracotta accent is part of the app's identity.
@Composable
fun ReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalReaderColors provides if (darkTheme) DarkReaderColors else LightReaderColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = ReaderShapes,
            content = content,
        )
    }
}
