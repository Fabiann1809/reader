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
    primary = Primary40,
    onPrimary = Color.White,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary30,
    inversePrimary = Primary80,
    secondary = Primary60,
    onSecondary = Color.White,
    secondaryContainer = Primary95,
    onSecondaryContainer = Primary30,
    tertiary = Ai40,
    onTertiary = Color.White,
    tertiaryContainer = Ai90,
    onTertiaryContainer = Ai40,
    background = Paper100,
    onBackground = Ink900,
    surface = Paper100,
    onSurface = Ink900,
    surfaceVariant = Paper200,
    onSurfaceVariant = Ink500,
    surfaceTint = Primary40,
    inverseSurface = Ink900,
    inverseOnSurface = Paper50,
    error = Error,
    onError = Color.White,
    outline = Paper300,
    outlineVariant = Paper300,
    surfaceBright = Paper50,
    surfaceDim = Paper200,
    surfaceContainerLowest = Paper50,
    surfaceContainerLow = Paper100,
    surfaceContainer = Paper200,
    surfaceContainerHigh = Paper300,
    surfaceContainerHighest = Paper300,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = DarkOnPrimary,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    inversePrimary = Primary40,
    secondary = Primary60,
    onSecondary = DarkOnPrimary,
    secondaryContainer = Primary30,
    onSecondaryContainer = Primary90,
    tertiary = Ai80,
    onTertiary = Ink900,
    tertiaryContainer = DarkSurfaceContainerHigh,
    onTertiaryContainer = Ai80,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = Ink300,
    surfaceTint = Primary80,
    inverseSurface = DarkOnSurface,
    inverseOnSurface = Ink900,
    error = Error,
    onError = Color.White,
    outline = Ink700,
    outlineVariant = DarkSurfaceContainerHigh,
    surfaceBright = DarkSurfaceContainerHigh,
    surfaceDim = DarkSurface,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHigh,
)

/** Design colors that have no Material 3 role: AI accent, library wood and semantic states. */
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
    val warning: Color,
    val info: Color,
    val covers: List<Color>,
)

private val LightReaderColors = ReaderColors(
    ai = Ai40,
    aiContainer = Ai90,
    aiSoft = Ai95,
    woodWall = Wood700,
    woodGrain = Wood600,
    shelfTop = listOf(ShelfTopA, ShelfTopB),
    shelfFront = listOf(ShelfFrontA, ShelfFrontB),
    progress = Primary60,
    success = Success,
    warning = Warning,
    info = Info,
    covers = CoverPalette,
)

private val DarkReaderColors = LightReaderColors.copy(
    ai = Ai80,
    aiContainer = DarkSurfaceContainerHigh,
    aiSoft = DarkSurfaceContainer,
    woodWall = Wood800,
    woodGrain = Wood700,
)

private val LocalReaderColors = staticCompositionLocalOf { LightReaderColors }

private val ReaderShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Accessors for the design colors that MaterialTheme doesn't cover. */
object ReaderTheme {
    val colors: ReaderColors
        @Composable
        @ReadOnlyComposable
        get() = LocalReaderColors.current
}

// Dynamic (wallpaper) color is intentionally not used: the forest green is part of the app's identity.
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
